package us.mikeandwan.photos.ui.screens.category

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.shared.toMediaGridItem

data class CategoryUiState(
    val category: Category? = null,
    val gridItems: List<MediaGridItem<Media>> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val filters: MediaFilters = MediaFilters(),
)

@HiltViewModel
class CategoryViewModel
    @Inject
    constructor(
        categoryRepository: CategoryRepository,
        private val mediaPreferenceRepository: MediaPreferenceRepository,
        private val mediaFavoriteService: MediaFavoriteService,
    ) : BaseCategoryViewModel(categoryRepository) {
        val uiState: StateFlow<CategoryUiState>

        init {
            val gridItemsFlow = filteredMedia
                .map { mediaList -> mediaList.map { it.toMediaGridItem() } }
                .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

            uiState = combine(
                category,
                gridItemsFlow,
                filters,
            ) { category, gridItems, filters ->
                var isError = false
                var isLoading = true

                if (category != null) {
                    isLoading = false
                }

                CategoryUiState(
                    category = category,
                    gridItems = gridItems,
                    isLoading = isLoading,
                    isError = isError,
                    filters = filters,
                )
            }.stateIn(viewModelScope, WhileSubscribed(5000), CategoryUiState())

            viewModelScope.launch {
                mediaPreferenceRepository
                    .getMediaPreference()
                    .collect { applyFilters(it.filters) }
            }
        }

        // saved rather than applied: what is saved comes back through the collection above, so the
        // menu here and the settings screen are the one setting
        fun setFilters(filters: MediaFilters) {
            viewModelScope.launch {
                mediaPreferenceRepository.setFilters(filters)
            }
        }

        fun initState(categoryId: Uuid) {
            loadCategory(categoryId)
            loadMedia(categoryId)
        }

        fun toggleFavorite(media: Media) {
            viewModelScope.launch {
                val isFavorite = mediaFavoriteService.setIsFavorite(media, !media.isFavorite)

                updateMedia(media.copy(isFavorite = isFavorite))
            }
        }
    }
