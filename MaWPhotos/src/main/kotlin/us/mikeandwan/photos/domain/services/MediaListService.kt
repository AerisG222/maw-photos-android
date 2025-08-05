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
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.PeriodicJob
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.MediaType
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid

class MediaListService @Inject constructor (
    private val categoryRepository: CategoryRepository,
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

    private val _category = MutableStateFlow<Category?>(null)
    val category = _category.asStateFlow()

    private val _activeIndex = MutableStateFlow(-1)
    val activeIndex = _activeIndex.asStateFlow()

    val activeMedia = combine(_media, activeIndex) { media, index ->
        media.getOrNull(index)
    }.stateIn(scope, WhileSubscribed(5000), null)

    val activeId = activeMedia
        .map { media -> media?.id ?: Uuid.NIL }
        .stateIn(scope, WhileSubscribed(5000), Uuid.NIL)

    fun setActiveIndex(index: Int) {
        _activeIndex.value = index
    }

    fun setActiveId(id: Uuid) {
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

    // FAVORITES
    val isFavorite = mediaRatingService.isFavorite

    fun setRating(isFavorite: Boolean) {
        activeMedia.value?.let {
            scope.launch {
                mediaRatingService.setIsFavorite(activeMedia.value!!, isFavorite)
            }
        }
    }

    // EXIF
    val exif = mediaExifService.exif

    fun fetchExif() {
        activeMedia.value?.let {
            scope.launch {
                mediaExifService.fetchExifDetails(it)
            }
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
                .collect { loadCategory(it.type, it.id) }
        }

        scope.launch {
            slideshowDurationInMillis
                .collect { _slideshowJob.setIntervalMillis(it) }
        }
    }

    private fun loadCategory(mediaType: MediaType, categoryId: Uuid) {
        if(category.value?.id == categoryId) {
            return
        }

        _category.value = null

        scope.launch {
            categoryRepository
                .getCategory(categoryId)
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
