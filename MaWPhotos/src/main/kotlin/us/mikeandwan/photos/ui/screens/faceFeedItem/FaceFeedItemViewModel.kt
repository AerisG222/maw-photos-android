package us.mikeandwan.photos.ui.screens.faceFeedItem

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.HttpDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.services.MediaListAction
import us.mikeandwan.photos.domain.services.MediaListService

// how close to the end of what has been loaded the pager gets before the next page is asked for
private const val PAGING_THRESHOLD = 4

data class FaceFeedItemUiState(
    // the category the active media belongs to.  a face feed spans categories, so this changes as
    // the pager moves rather than being fixed for the screen - it is what the title is drawn from.
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid = Uuid.NIL,
    val activeMedia: Media? = null,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val exif: JsonElement? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class FaceFeedItemViewModel
    @Inject
    constructor(
        private val faceFeedRepository: FaceFeedRepository,
        mediaPreferenceRepository: MediaPreferenceRepository,
        val videoPlayerDataSourceFactory: HttpDataSource.Factory,
        private val mediaListService: MediaListService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FaceFeedItemUiState())
        val uiState = _uiState.asStateFlow()

        init {
            val slideshowDurationInMillisFlow = mediaPreferenceRepository
                .getSlideshowIntervalSeconds()
                .map { seconds -> (seconds * 1000).toLong() }
                .stateIn(
                    viewModelScope,
                    WhileSubscribed(5000),
                    (MediaPreference().slideshowIntervalSeconds * 1000).toLong(),
                )

            mediaListService.initialize(
                faceFeedRepository.media,
                slideshowDurationInMillisFlow,
            )

            mediaListService.state
                .onEach { state ->
                    _uiState.update {
                        FaceFeedItemUiState(
                            category = state.category,
                            media = state.media,
                            activeId = state.activeId,
                            activeMedia = state.activeMedia,
                            isSlideshowPlaying = state.isSlideshowPlaying,
                            showDetailSheet = state.showDetailSheet,
                            exif = state.exif,
                            comments = state.comments,
                            isLoading = state.isLoading,
                        )
                    }

                    // the feed is paged, so swiping toward the end of what has been loaded has to
                    // fetch more or the pager simply stops at a page boundary
                    loadMoreIfNeeded(state.activeIndex, state.media.size)
                }.launchIn(viewModelScope)
        }

        /**
         * Points the shared feed at the subject being browsed, in case the pager is the first thing
         * on screen - a link opened into a feed, or a restore after process death, arrives here
         * without the grid having run.
         */
        fun initState(
            subject: FaceFeedSubject,
            mediaId: Uuid,
        ) {
            faceFeedRepository.initialize(subject)

            if (faceFeedRepository.media.value.isEmpty()) {
                loadNextPage()
            }

            mediaListService.onAction(MediaListAction.SetActiveId(mediaId))
        }

        fun setActiveId(id: Uuid) {
            mediaListService.onAction(MediaListAction.SetActiveId(id))
        }

        fun toggleSlideshow() {
            mediaListService.onAction(MediaListAction.ToggleSlideshow)
        }

        fun toggleShowDetails() {
            mediaListService.onAction(MediaListAction.ToggleShowDetails)
        }

        fun toggleFavorite() {
            _uiState.value.activeMedia?.let {
                mediaListService.onAction(MediaListAction.SetIsFavorite(!it.isFavorite))
            }
        }

        fun fetchExif() {
            mediaListService.onAction(MediaListAction.FetchExif)
        }

        fun fetchCommentDetails() {
            mediaListService.onAction(MediaListAction.FetchComments)
        }

        fun addComment(comment: String) {
            mediaListService.onAction(MediaListAction.AddComment(comment))
        }

        fun saveFileToShare(
            drawable: Drawable,
            filename: String,
            onComplete: (File) -> Unit,
        ) {
            mediaListService.onAction(
                MediaListAction.SaveFileToShare(drawable, filename, onComplete),
            )
        }

        fun reset() {
            mediaListService.onAction(MediaListAction.Reset)
        }

        private fun loadMoreIfNeeded(
            activeIndex: Int,
            loadedCount: Int,
        ) {
            if (activeIndex >= 0 && activeIndex >= loadedCount - PAGING_THRESHOLD) {
                loadNextPage()
            }
        }

        // nothing to do with the outcome here: a failure has already been reported the way every
        // other failed call is, and the pager simply stops at what it has
        private fun loadNextPage() {
            viewModelScope.launch {
                faceFeedRepository
                    .loadNextPage()
                    .collect { }
            }
        }
    }
