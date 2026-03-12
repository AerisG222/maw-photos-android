package us.mikeandwan.photos.ui.screens.categoryItem

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
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.services.MediaListAction
import us.mikeandwan.photos.domain.services.MediaListService
import us.mikeandwan.photos.ui.screens.category.BaseCategoryViewModel

data class CategoryItemUiState(
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid? = Uuid.NIL,
    val activeMedia: Media? = null,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val isAuthorized: Boolean = true,
    val exif: kotlinx.serialization.json.JsonElement? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
)

@HiltViewModel
class CategoryItemViewModel
    @Inject
    constructor(
        authGuard: AuthGuard,
        categoryRepository: CategoryRepository,
        mediaPreferenceRepository: MediaPreferenceRepository,
        val videoPlayerDataSourceFactory: HttpDataSource.Factory,
        private val mediaListService: MediaListService,
    ) : BaseCategoryViewModel(
            categoryRepository,
        ) {
        private val _uiState = MutableStateFlow(CategoryItemUiState())
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
                media,
                slideshowDurationInMillisFlow,
            )

            combine(
                mediaListService.state,
                authGuard.status,
            ) { mediaListState, authStatus ->
                CategoryItemUiState(
                    category = mediaListState.category,
                    media = mediaListState.media,
                    activeId = mediaListState.activeId,
                    activeMedia = mediaListState.activeMedia,
                    isSlideshowPlaying = mediaListState.isSlideshowPlaying,
                    showDetailSheet = mediaListState.showDetailSheet,
                    isAuthorized = authStatus != GuardStatus.Failed,
                    exif = mediaListState.exif,
                    comments = mediaListState.comments,
                    isLoading = mediaListState.isLoading,
                    hasPrevious = mediaListState.hasPrevious,
                    hasNext = mediaListState.hasNext,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

        fun initState(
            categoryId: Uuid,
            mediaId: Uuid,
        ) {
            mediaListService.onAction(MediaListAction.Reset)
            clear()
            _uiState.value = CategoryItemUiState()

            loadCategory(categoryId)
            loadMedia(categoryId)

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
                MediaListAction.SaveFileToShare(drawable, filename, onComplete)
            )
        }
    }
