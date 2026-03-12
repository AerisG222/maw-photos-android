package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.PeriodicJob
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media

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
        val isSlideshowPlaying = _slideshowJob.isRunning

        private val _showDetailSheet = MutableStateFlow(false)
        val showDetailSheet = _showDetailSheet.asStateFlow()

        private val _category = MutableStateFlow<Category?>(null)
        val category = _category.asStateFlow()

        private val _activeId = MutableStateFlow<Uuid?>(null)
        val activeId = _activeId.asStateFlow()

        val activeMedia = combine(_media, _activeId) { media, activeId ->
            media.firstOrNull { it.id == activeId }
        }

        fun setActiveId(id: Uuid) {
            _activeId.update { id }
        }

        fun toggleSlideshow() {
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

        fun toggleShowDetails() {
            if (_showDetailSheet.value) {
                if (_resumeSlideshowAfterShowingDetails.value) {
                    _slideshowJob.start()
                }
            } else {
                _resumeSlideshowAfterShowingDetails.value = isSlideshowPlaying.value
                _slideshowJob.stop()
            }

            _showDetailSheet.value = !_showDetailSheet.value
        }

        fun saveFileToShare(
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
        val isFavorite = mediaFavoriteService.isFavorite

        fun setIsFavorite(isFavorite: Boolean) {
            scope.launch {
                val currentMedia = activeMedia.firstOrNull() ?: return@launch

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
        val exif = mediaExifService.exif

        fun fetchExif() {
            scope.launch {
                activeMedia.firstOrNull()?.let { mediaExifService.fetchExifDetails(it) }
            }
        }

        // COMMENTS
        val comments = mediaCommentService.comments

        fun fetchComments() {
            scope.launch {
                activeMedia.firstOrNull()?.let { mediaCommentService.fetchCommentDetails(it) }
            }
        }

        fun addComment(comment: String) {
            scope.launch {
                activeMedia.firstOrNull()?.let { mediaCommentService.addComment(it, comment) }
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

            activeMedia
                .filterNotNull()
                .onEach { it ->
                    if (it.categoryId != category.value?.id) {
                        loadCategory(it.categoryId)
                    }
                }.launchIn(scope)

            slideshowDurationInMillis
                .onEach { _slideshowJob.setIntervalMillis(it) }
                .launchIn(scope)
        }

        private fun loadCategory(categoryId: Uuid) {
            if (category.value?.id == categoryId) return

            _category.value = null

            scope.launch {
                categoryRepository
                    .getCategory(categoryId)
                    .collect { _category.value = it }
            }
        }

        private fun moveNext() =
            flow<Unit> {
                val activeIndex = _media.value.indexOfFirst { it.id == activeId.value }
                val nextIndex = activeIndex + 1

                if (nextIndex < _media.value.size) {
                    setActiveId(_media.value[nextIndex].id)
                } else {
                    stopSlideshow()
                }
            }
    }
