package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import com.hoc081098.flowext.combine
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    data object Reset : MediaListAction()

    data class SetActiveId(
        val id: Uuid,
    ) : MediaListAction()

    data object ToggleSlideshow : MediaListAction()

    data object ToggleShowDetails : MediaListAction()

    data class SetIsFavorite(
        val isFavorite: Boolean,
    ) : MediaListAction()

    data object FetchExif : MediaListAction()

    data object FetchComments : MediaListAction()

    data class AddComment(
        val comment: String,
    ) : MediaListAction()

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
        private val category = MutableStateFlow<Category?>(null)
        private val media = MutableStateFlow<List<Media>>(emptyList())
        private val activeId = MutableStateFlow<Uuid?>(null)
        private val slideshowJob = PeriodicJob { moveNext() }
        private val resumeSlideshowAfterShowingDetails = MutableStateFlow(false)
        private val showDetailSheet = MutableStateFlow(false)

        val state: StateFlow<MediaListState> =
            combine(
                category,
                media,
                activeId,
                slideshowJob.isRunning,
                showDetailSheet,
                mediaExifService.exif,
                mediaCommentService.comments,
            ) {
                category,
                media,
                activeId,
                isSlideshowPlaying,
                showDetailSheet,
                exif,
                comments,
                ->
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
                is MediaListAction.Reset -> {
                    reset()
                }

                is MediaListAction.SetActiveId -> {
                    setActiveId(action.id)
                }

                is MediaListAction.ToggleSlideshow -> {
                    toggleSlideshow()
                }

                is MediaListAction.ToggleShowDetails -> {
                    toggleShowDetails()
                }

                is MediaListAction.SetIsFavorite -> {
                    setIsFavorite(action.isFavorite)
                }

                is MediaListAction.FetchExif -> {
                    fetchExif()
                }

                is MediaListAction.FetchComments -> {
                    fetchComments()
                }

                is MediaListAction.AddComment -> {
                    addComment(action.comment)
                }

                is MediaListAction.SaveFileToShare -> {
                    saveFileToShare(action.drawable, action.filename, action.onComplete)
                }
            }
        }

        private fun reset() {
            slideshowJob.stop()
            category.value = null
            media.value = emptyList()
            activeId.value = null
            showDetailSheet.value = false
            resumeSlideshowAfterShowingDetails.value = false
        }

        private fun setActiveId(id: Uuid) {
            activeId.update { id }
        }

        private fun toggleSlideshow() {
            if (slideshowJob.isRunning.value) {
                stopSlideshow()
            } else {
                startSlideshow()
            }
        }

        private fun startSlideshow() {
            slideshowJob.start()
        }

        private fun stopSlideshow() {
            slideshowJob.stop()
        }

        private fun toggleShowDetails() {
            if (showDetailSheet.value) {
                if (resumeSlideshowAfterShowingDetails.value) {
                    slideshowJob.start()
                }
            } else {
                resumeSlideshowAfterShowingDetails.value = slideshowJob.isRunning.value
                slideshowJob.stop()
            }

            showDetailSheet.value = !showDetailSheet.value
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
                val updatedMedia = media.value.toMutableList()
                val currentIndex = updatedMedia.indexOfFirst { it.id == currentMedia.id }

                if (
                    currentIndex >= 0 &&
                    currentIndex < updatedMedia.size &&
                    updatedMedia[currentIndex].id == currentMedia.id
                ) {
                    val updatedItem = currentMedia.copy(isFavorite = resultIsFav)
                    updatedMedia[currentIndex] = updatedItem
                    media.value = updatedMedia
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
            sourceMedia: StateFlow<List<Media>>,
            slideshowDurationInMillis: StateFlow<Long>,
        ) {
            category.value = null

            sourceMedia
                .onEach { media.value = it }
                .launchIn(scope)

            state
                .map { it.activeMedia }
                .filterNotNull()
                .onEach { activeMedia ->
                    if (activeMedia.categoryId != category.value?.id) {
                        loadCategory(activeMedia.categoryId)
                    }
                }.launchIn(scope)

            slideshowDurationInMillis
                .onEach { slideshowJob.setIntervalMillis(it) }
                .launchIn(scope)
        }

        private fun loadCategory(categoryId: Uuid) {
            if (category.value?.id == categoryId) return

            category.value = null

            scope.launch {
                categoryRepository
                    .getCategory(categoryId)
                    .collect { category.value = it }
            }
        }

        private fun moveNext() =
            flow<Unit> {
                val activeIndex = media.value.indexOfFirst { it.id == activeId.value }
                val nextIndex = activeIndex + 1

                if (nextIndex < media.value.size) {
                    setActiveId(media.value[nextIndex].id)
                } else {
                    stopSlideshow()
                }
            }
    }
