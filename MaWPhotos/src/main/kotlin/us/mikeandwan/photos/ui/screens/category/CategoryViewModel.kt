package us.mikeandwan.photos.ui.screens.category

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.shared.toMediaGridItem

data class CategoryUiState(
    val category: Category? = null,
    val gridItems: List<MediaGridItem<Media>> = emptyList(),
    val gridItemThumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

@HiltViewModel
class CategoryViewModel
    @Inject
    constructor(
        categoryRepository: CategoryRepository,
        mediaPreferenceRepository: MediaPreferenceRepository,
    ) : BaseCategoryViewModel(categoryRepository) {
        private val _uiState = MutableStateFlow(CategoryUiState())
        val uiState = _uiState.asStateFlow()

        init {
            val gridItemThumbnailSizeFlow = mediaPreferenceRepository
                .getPhotoGridItemSize()
                .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Unspecified)

            val gridItemsFlow = combine(
                media,
                gridItemThumbnailSizeFlow,
                mediaPreferenceRepository.getMediaPreference(),
            ) { mediaList, thumbnailSize, mediaPref ->
                mediaList.map {
                    it.toMediaGridItem(
                        useLargeTeaser = thumbnailSize == GridThumbnailSize.Large,
                        showMediaTypeIndicator = mediaPref.showMediaTypeIndicator,
                    )
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

            combine(
                category,
                gridItemsFlow,
                gridItemThumbnailSizeFlow,
            ) { category, gridItems, gridItemThumbnailSize ->
                var isError = false
                var isLoading = true

                if (category != null) {
                    isLoading = false
                }

                CategoryUiState(
                    category = category,
                    gridItems = gridItems,
                    gridItemThumbnailSize = gridItemThumbnailSize,
                    isLoading = isLoading,
                    isError = isError,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

        fun initState(categoryId: Uuid) {
            loadCategory(categoryId)
            loadMedia(categoryId)
        }
    }
