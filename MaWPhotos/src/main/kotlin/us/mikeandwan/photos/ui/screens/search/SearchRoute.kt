package us.mikeandwan.photos.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarFilterMenu
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
data class SearchNavKey(
    val searchTerm: String? = null,
) : NavKey

fun EntryProviderScope<NavKey>.search() {
    entry<SearchNavKey> { args ->
        SearchRoute(
            initialSearchTerm = args.searchTerm,
        )
    }
}

@Composable
private fun SearchRoute(
    initialSearchTerm: String?,
    vm: SearchViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Search)
    }

    LaunchedEffect(initialSearchTerm) {
        if (!initialSearchTerm.isNullOrBlank()) {
            vm.search(initialSearchTerm)
        }
    }

    // what the bar opens with rather than what it holds now: the bar keeps its own text and only
    // takes this when it changes, so pushing the same term again leaves a cleared bar cleared
    val barTerm = remember(initialSearchTerm) {
        if (initialSearchTerm.isNullOrBlank()) uiState.activeTerm else initialSearchTerm
    }
    val hasSearch = uiState.activeTerm.isNotBlank()

    // nothing to narrow until there is a search
    LaunchedEffect(barTerm, hasSearch, uiState.filters) {
        appActions.updateTopBar(
            NavigationArea.Search,
            TopBarState(
                showSearch = true,
                initialSearchTerm = barTerm,
                filterMenu = if (hasSearch) TopBarFilterMenu(uiState.filters, vm::setFilters) else null,
            ),
        )
    }

    SearchScreen(
        uiState = uiState,
        onNavigateToCategory = { category ->
            appActions.navigateToCategory(category.id)
        },
        onToggleFavorite = { category -> vm.toggleFavorite(category) },
        onContinueSearch = { vm.continueSearch() },
        onRetrySearch = { vm.retrySearch() },
        onSetFilters = { vm.setFilters(it) },
    )
}
