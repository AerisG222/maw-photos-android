package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaCategoryRepository
import us.mikeandwan.photos.domain.PeriodicJob
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaCategory
import us.mikeandwan.photos.domain.models.MediaType
import java.io.File
import javax.inject.Inject

class MediaListService @Inject constructor (
    private val mediaCategoryRepository: MediaCategoryRepository,
    private val fileRepository: FileStorageRepository,
    private val mediaRatingService: MediaRatingService,
    private val mediaCommentService: MediaCommentService,
    private val mediaExifService: MediaExifService
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _media = MutableStateFlow<List<Media>>(emptyList())

    private val _slideshowJob = PeriodicJob { moveNext() }

    private val _resumeSlideshowAfterShowingDetails = MutableStateFlow(false)
    val isSlideshowPlaying = _slideshowJob.doJob

    private val _showDetailSheet = MutableStateFlow(false)
    val showDetailSheet = _showDetailSheet.asStateFlow()

    private val _category = MutableStateFlow<MediaCategory?>(null)
    val category = _category.asStateFlow()

    private val _activeIndex = MutableStateFlow(-1)
    val activeIndex = _activeIndex.asStateFlow()

    val activeMedia = combine(_media, activeIndex) { media, index ->
        media.getOrNull(index)
    }.stateIn(scope, WhileSubscribed(5000), null)

    val activeId = activeMedia
        .map { media -> media?.id ?: -1 }
        .stateIn(scope, WhileSubscribed(5000), -1)

    fun setActiveIndex(index: Int) {
        _activeIndex.value = index
    }

    fun setActiveId(id: Int) {
        setActiveIndex(_media.value.indexOfFirst { it.id == id })
    }

    fun toggleSlideshow() {
        if(_slideshowJob.doJob.value) {
            stopSlideshow()
        } else {
            startSlideshow()
        }
    }

    private fun startSlideshow() { _slideshowJob.start() }
    private fun stopSlideshow() { _slideshowJob.stop() }

    fun toggleShowDetails() {
        if(_showDetailSheet.value) {
            // detail sheet to be hidden
            if(_resumeSlideshowAfterShowingDetails.value) {
                _slideshowJob.start()
            }
        } else {
            _resumeSlideshowAfterShowingDetails.value = isSlideshowPlaying.value
            _slideshowJob.stop()
        }

        _showDetailSheet.value = !_showDetailSheet.value
    }

    fun saveFileToShare(drawable: Drawable, filename: String, onComplete: (File) -> Unit) {
        scope.launch {
            val file = fileRepository.savePhotoToShare(drawable, filename)

            onComplete(file)
        }
    }

    // TODO: pass in media rather than relying on activeMedia?

    // RATINGS
    val userRating = mediaRatingService.userRating
    val averageRating = mediaRatingService.averageRating

    fun setRating(rating: Short) {
        activeMedia.value?.let {
            scope.launch {
                mediaRatingService.setRating(activeMedia.value!!, rating)
            }
        }
    }

    fun fetchRating() {
        activeMedia.value?.let {
            scope.launch {
                mediaRatingService.fetchRatingDetails(activeMedia.value!!)
            }
        }
    }

    // EXIF
    val exif = mediaExifService.exif

    fun fetchExif() {
        activeMedia.value?.let {
//            if(it !is Video) {
                scope.launch {
                    mediaExifService.fetchExifDetails(it)
                }
//            }
        }
    }

    // COMMENTS
    val comments = mediaCommentService.comments

    fun fetchComments() {
        activeMedia.value?.let {
            scope.launch {
                mediaCommentService.fetchCommentDetails(activeMedia.value!!)
            }
        }
    }

    fun addComment(comment: String) {
        activeMedia.value?.let {
            scope.launch {
                mediaCommentService.addComment(activeMedia.value!!, comment)
            }
        }
    }

    fun initialize(
        media: StateFlow<List<Media>>,
        slideshowDurationInMillis: StateFlow<Long>
    ) {
        scope.launch {
            media
                .collect { _media.value = it }
        }

        scope.launch {
            activeMedia
                .filterNotNull()
                .collect { loadCategory(it.type, it.categoryId) }
        }

        scope.launch {
            slideshowDurationInMillis
                .collect { _slideshowJob.setIntervalMillis(it) }
        }
    }

    private fun loadCategory(mediaType: MediaType, categoryId: Int) {
        if(category.value?.id == categoryId) {
            return
        }

        _category.value = null

        scope.launch {
            mediaCategoryRepository
                .getCategory(mediaType, categoryId)
                .collect { _category.value = it }
        }
    }

    private fun moveNext() = flow<Unit> {
        val nextIndex = activeIndex.value + 1

        if(nextIndex < _media.value.size) {
            setActiveIndex(nextIndex)
        } else {
            stopSlideshow()
        }
    }
}
