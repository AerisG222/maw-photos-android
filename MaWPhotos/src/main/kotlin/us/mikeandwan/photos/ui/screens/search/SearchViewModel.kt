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
        private val searchPreferenceRepository: SearchPreferenceRepository,
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

        init {
            // one launch per change, so a new set of filters does not wait on the page the last set
            // asked for: the repository throws that page away when it lands
            viewModelScope.launch {
                searchPreferenceRepository
                    .getFilters()
                    .collect { filters -> applyFilters(filters) }
            }
        }

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

        // saved rather than applied: what is saved comes back through the collection above, so the
        // menu here and the settings screen are the one setting
        fun setFilters(filters: MediaFilters) {
            viewModelScope.launch {
                searchPreferenceRepository.setFilters(filters)
            }
        }

        private fun applyFilters(filters: MediaFilters) {
            viewModelScope.launch {
                searchRepository
                    .applyFilters(filters)
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
