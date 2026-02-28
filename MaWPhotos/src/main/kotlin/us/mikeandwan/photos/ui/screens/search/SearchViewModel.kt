package us.mikeandwan.photos.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.flowext.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.SearchSource

data class SearchUiState(
    val results: List<Category> = emptyList(),
    val hasMore: Boolean = false,
    val displayType: CategoryDisplayType = CategoryDisplayType.Unspecified,
    val thumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val isAuthorized: Boolean = true,
    val activeTerm: String = "",
)

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        authGuard: AuthGuard,
        private val searchRepository: SearchRepository,
        searchPreferenceRepository: SearchPreferenceRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState = _uiState.asStateFlow()

        init {
            combine(
                searchRepository.searchResults,
                searchRepository.hasMoreResults,
                searchPreferenceRepository.getSearchDisplayType(),
                searchPreferenceRepository.getSearchGridItemSize(),
                authGuard.status,
                searchRepository.activeSearchTerm,
            ) { results, hasMore, displayType, thumbSize, authStatus, activeTerm ->
                SearchUiState(
                    results = results,
                    hasMore = hasMore,
                    displayType = displayType,
                    thumbnailSize = thumbSize,
                    isAuthorized = authStatus !is GuardStatus.Failed,
                    activeTerm = activeTerm,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
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

        fun continueSearch() {
            viewModelScope.launch {
                searchRepository
                    .continueSearch()
                    .collect { }
            }
        }
    }
