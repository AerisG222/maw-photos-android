package us.mikeandwan.photos.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.SearchSource
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    authGuard: AuthGuard,
    private val searchRepository: SearchRepository,
    searchPreferenceRepository: SearchPreferenceRepository
) : ViewModel() {
    val activeTerm = searchRepository.activeSearchTerm
    val hasMore = searchRepository.hasMoreResults
    val searchResults = searchRepository.searchResults

    val displayType = searchPreferenceRepository
        .getSearchDisplayType()
        .stateIn(viewModelScope, WhileSubscribed(5000), CategoryDisplayType.Unspecified)

    val gridItemThumbnailSize = searchPreferenceRepository
        .getSearchGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    val isAuthorized = authGuard.status
        .map {
            when(it) {
                is GuardStatus.Failed -> false
                else -> true
            }
        }.stateIn(viewModelScope, WhileSubscribed(5000), true)

    fun search(term: String) {
        viewModelScope.launch {
            searchRepository
                .performSearch(
                    query = term,
                    searchSource = SearchSource.SearchMenu
                )
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
