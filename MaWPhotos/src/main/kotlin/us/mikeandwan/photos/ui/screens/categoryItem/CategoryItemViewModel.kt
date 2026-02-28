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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.services.MediaListService
import us.mikeandwan.photos.ui.screens.category.BaseCategoryViewModel

data class CategoryItemUiState(
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

        private val initialMediaId = MutableStateFlow(Uuid.NIL)
        private val initialMediaIdWasSet = MutableStateFlow(false)

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

            viewModelScope.launch {
                combine(
                    media,
                    initialMediaId,
                    initialMediaIdWasSet,
                ) { media, id, wasSet ->
                    if (wasSet || media.isEmpty() || id == Uuid.NIL) return@combine

                    mediaListService.setActiveId(id)
                    initialMediaIdWasSet.value = true
                }.collect { }
            }

            combine(
                mediaListService.category,
                mediaListService.activeMedia,
                mediaListService.activeId,
                mediaListService.activeIndex,
                mediaListService.isSlideshowPlaying,
                mediaListService.showDetailSheet,
                authGuard.status,
                mediaListService.exif,
                mediaListService.comments,
                media
            ) { args: Array<Any?> ->
                CategoryItemUiState(
                    category = args[0] as? Category,
                    activeMedia = args[1] as? Media,
                    activeId = args[2] as Uuid,
                    activeIndex = args[3] as Int,
                    isSlideshowPlaying = args[4] as Boolean,
                    showDetailSheet = args[5] as Boolean,
                    isAuthorized = args[6] !is GuardStatus.Failed,
                    exif = args[7] as? JsonElement,
                    comments = @Suppress("UNCHECKED_CAST") (args[8] as List<Comment>),
                    media = @Suppress("UNCHECKED_CAST") (args[9] as List<Media>),
                    isLoading = @Suppress("UNCHECKED_CAST") (args[9] as List<Media>).isEmpty() || (args[2] as Uuid) == Uuid.NIL
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

        fun initState(
            categoryId: Uuid,
            mediaId: Uuid,
        ) {
            loadCategory(categoryId)
            loadMedia(categoryId)

            initialMediaId.value = mediaId
        }

        fun setActiveIndex(index: Int) {
            mediaListService.setActiveIndex(index)
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
