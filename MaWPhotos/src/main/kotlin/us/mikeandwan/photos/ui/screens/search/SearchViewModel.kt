package us.mikeandwan.photos.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.flowext.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.SearchSource

data class SearchUiState(
    val results: List<Category> = emptyList(),
    val hasMore: Boolean = false,
    val displayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val activeTerm: String = "",
    val loadFailed: Boolean = false,
    val isLoading: Boolean = false,
    val filters: MediaFilters = MediaFilters(),
)

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
        private val categoryRepository: CategoryRepository,
        searchPreferenceRepository: SearchPreferenceRepository,
    ) : ViewModel() {
        val uiState =
            combine(
                searchRepository.searchResults,
                searchRepository.hasMoreResults,
                searchPreferenceRepository.getSearchDisplayType(),
                searchRepository.activeSearchTerm,
                searchRepository.loadFailed,
                searchRepository.isLoading,
                searchRepository.filters,
            ) { results, hasMore, displayType, activeTerm, loadFailed, isLoading, filters ->
                SearchUiState(
                    results = results,
                    hasMore = hasMore,
                    displayType = displayType,
                    activeTerm = activeTerm,
                    loadFailed = loadFailed,
                    isLoading = isLoading,
                    filters = filters,
                )
            }.stateIn(viewModelScope, WhileSubscribed(5000), SearchUiState())

        fun search(term: String) {
            viewModelScope.launch {
                searchRepository
                    .performSearch(
                        query = term,
                        searchSource = SearchSource.SearchMenu,
                    ).collect { }
            }
        }

        fun toggleFavorite(category: Category) {
            viewModelScope.launch {
                categoryRepository
                    .setFavorite(category.id, !category.isFavorite)
                    .filterIsInstance<ExternalCallStatus.Success<Category>>()
                    .catch { e -> Timber.e(e) }
                    .collect { searchRepository.updateCategory(it.result) }
            }
        }

        fun setFilters(filters: MediaFilters) {
            viewModelScope.launch {
                searchRepository
                    .setFilters(filters)
                    .collect { }
            }
        }

        fun retrySearch() {
            viewModelScope.launch {
                searchRepository
                    .retrySearch()
                    .collect { }
            }
        }

        fun continueSearch() {
            viewModelScope.launch {
                searchRepository
                    .continueSearch()
                    .collect { }
            }
        }
    }
