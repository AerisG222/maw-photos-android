package us.mikeandwan.photos.ui.screens.randomItem

import android.graphics.drawable.Drawable
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.HttpDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.RandomPreference
import us.mikeandwan.photos.domain.services.MediaListService
import us.mikeandwan.photos.ui.screens.random.BaseRandomViewModel

data class RandomItemUiState(
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid = Uuid.NIL,
    val activeIndex: Int = -1,
    val activeMedia: Media? = null,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val isAuthorized: Boolean = true,
    val exif: JsonElement? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class RandomItemViewModel
    @Inject
    constructor(
        authGuard: AuthGuard,
        randomMediaRepository: RandomMediaRepository,
        randomPreferenceRepository: RandomPreferenceRepository,
        val videoPlayerDataSourceFactory: HttpDataSource.Factory,
        private val mediaListService: MediaListService,
    ) : BaseRandomViewModel(
            randomMediaRepository,
        ) {
        private val _uiState = MutableStateFlow(RandomItemUiState())
        val uiState = _uiState.asStateFlow()

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

            combine(
                mediaListService.state,
                authGuard.status,
            ) { mediaListState, authStatus ->
                RandomItemUiState(
                    category = mediaListState.category,
                    media = mediaListState.media,
                    activeId = mediaListState.activeId ?: Uuid.NIL,
                    activeIndex = mediaListState.activeIndex,
                    activeMedia = mediaListState.activeMedia,
                    isSlideshowPlaying = mediaListState.isSlideshowPlaying,
                    showDetailSheet = mediaListState.showDetailSheet,
                    isAuthorized = authStatus != GuardStatus.Failed,
                    exif = mediaListState.exif,
                    comments = mediaListState.comments,
                    isLoading =
                        mediaListState.media.isEmpty() || mediaListState.activeId == null,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

        fun initState(id: Uuid) {
            mediaListService.setActiveId(id)
        }

        fun setActiveId(id: Uuid) {
            mediaListService.setActiveId(id)
        }

        fun toggleSlideshow() {
            mediaListService.toggleSlideshow()
        }

        fun toggleShowDetails() {
            mediaListService.toggleShowDetails()
        }

        fun toggleFavorite() {
            _uiState.value.activeMedia?.let {
                mediaListService.setIsFavorite(!it.isFavorite)
            }
        }

        fun fetchExif() {
            mediaListService.fetchExif()
        }

        fun fetchCommentDetails() {
            mediaListService.fetchComments()
        }

        fun addComment(comment: String) {
            mediaListService.addComment(comment)
        }

        fun saveFileToShare(
            drawable: Drawable,
            filename: String,
            onComplete: (File) -> Unit,
        ) {
            mediaListService.saveFileToShare(drawable, filename, onComplete)
        }
    }
