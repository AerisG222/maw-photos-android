package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.PeriodicJob
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media

sealed class MediaListAction {
    data class SetActiveId(val id: Uuid) : MediaListAction()
    data object ToggleSlideshow : MediaListAction()
    data object ToggleShowDetails : MediaListAction()
    data class SetIsFavorite(val isFavorite: Boolean) : MediaListAction()
    data object FetchExif : MediaListAction()
    data object FetchComments : MediaListAction()
    data class AddComment(val comment: String) : MediaListAction()
    data class SaveFileToShare(
        val drawable: Drawable,
        val filename: String,
        val onComplete: (File) -> Unit,
    ) : MediaListAction()
}

data class MediaListState(
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid? = null,
    val activeMedia: Media? = null,
    val activeIndex: Int = -1,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val exif: JsonElement? = null,
    val comments: List<Comment> = emptyList(),
) {
    val isLoading: Boolean
        get() = category == null || media.isEmpty() || activeId == null || activeMedia == null

    val hasPrevious: Boolean
        get() = activeIndex > 0

    val hasNext: Boolean
        get() = activeIndex >= 0 && activeIndex < media.size - 1
}

class MediaListService
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val fileRepository: FileStorageRepository,
        private val mediaFavoriteService: MediaFavoriteService,
        private val mediaCommentService: MediaCommentService,
        private val mediaExifService: MediaExifService,
    ) {
        private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        private val _media = MutableStateFlow<List<Media>>(emptyList())

        private val _slideshowJob = PeriodicJob { moveNext() }

        private val _resumeSlideshowAfterShowingDetails = MutableStateFlow(false)

        private val _showDetailSheet = MutableStateFlow(false)

        private val _category = MutableStateFlow<Category?>(null)

        private val _activeId = MutableStateFlow<Uuid?>(null)

        val state: StateFlow<MediaListState> =
            combine(
                _category,
                _media,
                _activeId,
                _slideshowJob.isRunning,
                _showDetailSheet,
                mediaExifService.exif,
                mediaCommentService.comments,
            ) { args: Array<Any?> ->
                val category = args[0] as? Category
                @Suppress("UNCHECKED_CAST")
                val media = args[1] as List<Media>
                val activeId = args[2] as? Uuid
                val isSlideshowPlaying = args[3] as Boolean
                val showDetailSheet = args[4] as Boolean
                val exif = args[5] as? JsonElement
                @Suppress("UNCHECKED_CAST")
                val comments = args[6] as List<Comment>

                val activeIndex = media.indexOfFirst { it.id == activeId }
                MediaListState(
                    category = category,
                    media = media,
                    activeId = activeId,
                    activeMedia = if (activeIndex >= 0) media[activeIndex] else null,
                    activeIndex = activeIndex,
                    isSlideshowPlaying = isSlideshowPlaying,
                    showDetailSheet = showDetailSheet,
                    exif = exif,
                    comments = comments,
                )
            }.stateIn(scope, SharingStarted.Eagerly, MediaListState())

        fun onAction(action: MediaListAction) {
            when (action) {
                is MediaListAction.SetActiveId -> setActiveId(action.id)
                is MediaListAction.ToggleSlideshow -> toggleSlideshow()
                is MediaListAction.ToggleShowDetails -> toggleShowDetails()
                is MediaListAction.SetIsFavorite -> setIsFavorite(action.isFavorite)
                is MediaListAction.FetchExif -> fetchExif()
                is MediaListAction.FetchComments -> fetchComments()
                is MediaListAction.AddComment -> addComment(action.comment)
                is MediaListAction.SaveFileToShare ->
                    saveFileToShare(action.drawable, action.filename, action.onComplete)
            }
        }

        private fun setActiveId(id: Uuid) {
            _activeId.update { id }
        }

        private fun toggleSlideshow() {
            if (_slideshowJob.isRunning.value) {
                stopSlideshow()
            } else {
                startSlideshow()
            }
        }

        private fun startSlideshow() {
            _slideshowJob.start()
        }

        private fun stopSlideshow() {
            _slideshowJob.stop()
        }

        private fun toggleShowDetails() {
            if (_showDetailSheet.value) {
                if (_resumeSlideshowAfterShowingDetails.value) {
                    _slideshowJob.start()
                }
            } else {
                _resumeSlideshowAfterShowingDetails.value = _slideshowJob.isRunning.value
                _slideshowJob.stop()
            }

            _showDetailSheet.value = !_showDetailSheet.value
        }

        private fun saveFileToShare(
            drawable: Drawable,
            filename: String,
            onComplete: (File) -> Unit,
        ) {
            scope.launch {
                val file = fileRepository.savePhotoToShare(drawable, filename)
                onComplete(file)
            }
        }

        // FAVORITES
        private fun setIsFavorite(isFavorite: Boolean) {
            scope.launch {
                val currentMedia = state.value.activeMedia ?: return@launch

                val resultIsFav = mediaFavoriteService.setIsFavorite(currentMedia, isFavorite)

                // Update the media list safely
                val updatedMedia = _media.value.toMutableList()
                val currentIndex = updatedMedia.indexOfFirst { it.id == currentMedia.id }

                if (
                    currentIndex >= 0 &&
                    currentIndex < updatedMedia.size &&
                    updatedMedia[currentIndex].id == currentMedia.id
                ) {
                    val updatedItem = currentMedia.copy(isFavorite = resultIsFav)
                    updatedMedia[currentIndex] = updatedItem
                    _media.value = updatedMedia
                    categoryRepository.tryUpdateCache(updatedItem)
                }
            }
        }

        // EXIF
        private fun fetchExif() {
            scope.launch {
                state.value.activeMedia?.let { mediaExifService.fetchExifDetails(it) }
            }
        }

        // COMMENTS
        private fun fetchComments() {
            scope.launch {
                state.value.activeMedia?.let { mediaCommentService.fetchCommentDetails(it) }
            }
        }

        private fun addComment(comment: String) {
            scope.launch {
                state.value.activeMedia?.let { mediaCommentService.addComment(it, comment) }
            }
        }

        fun initialize(
            media: StateFlow<List<Media>>,
            slideshowDurationInMillis: StateFlow<Long>,
        ) {
            _category.value = null

            media
                .onEach { _media.value = it }
                .launchIn(scope)

            state
                .map { it.activeMedia }
                .filterNotNull()
                .onEach { activeMedia ->
                    if (activeMedia.categoryId != _category.value?.id) {
                        loadCategory(activeMedia.categoryId)
                    }
                }.launchIn(scope)

            slideshowDurationInMillis
                .onEach { _slideshowJob.setIntervalMillis(it) }
                .launchIn(scope)
        }

        private fun loadCategory(categoryId: Uuid) {
            if (_category.value?.id == categoryId) return

            _category.value = null

            scope.launch {
                categoryRepository
                    .getCategory(categoryId)
                    .collect { _category.value = it }
            }
        }

        private fun moveNext() =
            flow<Unit> {
                val activeIndex = _media.value.indexOfFirst { it.id == _activeId.value }
                val nextIndex = activeIndex + 1

                if (nextIndex < _media.value.size) {
                    setActiveId(_media.value[nextIndex].id)
                } else {
                    stopSlideshow()
                }
            }
    }
