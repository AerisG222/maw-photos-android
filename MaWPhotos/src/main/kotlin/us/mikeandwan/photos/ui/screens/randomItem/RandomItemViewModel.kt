package us.mikeandwan.photos.ui.screens.randomItem

import android.graphics.drawable.Drawable
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.HttpDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.FaceHighlight
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFaces
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.RandomPreference
import us.mikeandwan.photos.domain.services.MediaListAction
import us.mikeandwan.photos.domain.services.MediaListService
import us.mikeandwan.photos.ui.screens.random.BaseRandomViewModel

data class RandomItemUiState(
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid = Uuid.NIL,
    val activeMedia: Media? = null,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val exif: JsonElement? = null,
    val comments: List<Comment> = emptyList(),
    val faces: List<FaceHighlight> = emptyList(),
    // everyone the pipeline found here, which is not the same as the boxes above: the
    // details sheet names them whether or not the overlay is drawing anything
    val mediaFaces: MediaFaces? = null,
    val places: List<Place>? = null,
    val showFaceHighlights: Boolean = false,
    val canHighlightFaces: Boolean = false,
    val isLoading: Boolean = true,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val activeIndex: Int
        get() = media.indexOfFirst { it.id == activeId }
}

@HiltViewModel
class RandomItemViewModel
    @Inject
    constructor(
        randomMediaRepository: RandomMediaRepository,
        randomPreferenceRepository: RandomPreferenceRepository,
        val videoPlayerDataSourceFactory: HttpDataSource.Factory,
        private val mediaListService: MediaListService,
    ) : BaseRandomViewModel(
            randomMediaRepository,
        ) {
        val uiState: StateFlow<RandomItemUiState>

        init {
            val slideshowDurationInMillisFlow = randomPreferenceRepository
                .getSlideshowIntervalSeconds()
                .map { seconds -> (seconds * 1000).toLong() }
                .stateIn(
                    viewModelScope,
                    WhileSubscribed(5000),
                    (RandomPreference().slideshowIntervalSeconds * 1000).toLong(),
                )

            mediaListService.initialize(
                media,
                slideshowDurationInMillisFlow,
            )

            uiState = mediaListService.state
                .map { mediaListState ->
                    RandomItemUiState(
                        category = mediaListState.category,
                        media = mediaListState.media,
                        activeId = mediaListState.activeId,
                        activeMedia = mediaListState.activeMedia,
                        isSlideshowPlaying = mediaListState.isSlideshowPlaying,
                        showDetailSheet = mediaListState.showDetailSheet,
                        exif = mediaListState.exif,
                        comments = mediaListState.comments,
                        faces = mediaListState.facesToHighlight,
                        mediaFaces = mediaListState.mediaFaces,
                        places = mediaListState.places,
                        showFaceHighlights = mediaListState.showFaceHighlights,
                        canHighlightFaces = mediaListState.canHighlightFaces,
                        isLoading = mediaListState.isLoading,
                        hasPrevious = mediaListState.hasPrevious,
                        hasNext = mediaListState.hasNext,
                    )
                }.stateIn(viewModelScope, WhileSubscribed(5000), RandomItemUiState())
        }

        fun reset() {
            setActiveId(Uuid.NIL)
        }

        fun initState(id: Uuid) {
            mediaListService.onAction(MediaListAction.SetActiveId(id))
        }

        // the service keeps its own scope over flows that outlive this screen - the feed, the
        // preferences, the granted scopes - so it has to be told when this view model is done with
        // it, or it stays subscribed for the rest of the session
        override fun onCleared() {
            mediaListService.close()
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

        fun toggleFaceHighlights() {
            mediaListService.onAction(MediaListAction.ToggleFaceHighlights)
        }

        fun toggleFavorite() {
            // read from the service rather than from [uiState], which stops assembling once the
            // screen stops collecting it
            mediaListService.state.value.activeMedia?.let {
                mediaListService.onAction(MediaListAction.SetIsFavorite(!it.isFavorite))
            }
        }

        fun fetchExif() {
            mediaListService.onAction(MediaListAction.FetchExif)
        }

        fun fetchCommentDetails() {
            mediaListService.onAction(MediaListAction.FetchComments)
        }

        fun fetchFaces() {
            mediaListService.onAction(MediaListAction.FetchFaces)
        }

        fun fetchPlaces() {
            mediaListService.onAction(MediaListAction.FetchPlaces)
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
    }
