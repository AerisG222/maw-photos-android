package us.mikeandwan.photos.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
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

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Search)
    }

    LaunchedEffect(initialSearchTerm) {
        var term = initialSearchTerm

        if (term.isNullOrBlank()) {
            term = uiState.activeTerm
        } else {
            vm.search(term)
        }

        appActions.updateTopBar(
            NavigationArea.Search,
            TopBarState(
                showSearch = true,
                initialSearchTerm = term,
            ),
        )
    }

    SearchScreen(
        uiState = uiState,
        onNavigateToCategory = { category ->
            appActions.navigateToCategory(category.id)
        },
        onContinueSearch = { vm.continueSearch() },
    )
}
