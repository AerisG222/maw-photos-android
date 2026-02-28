package us.mikeandwan.photos.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
data class SearchRoute(
    val searchTerm: String? = null,
) : NavKey

fun EntryProviderScope<NavKey>.search(
    navigateToCategory: (Category) -> Unit,
    navigateToLogin: () -> Unit,
) {
    entry<SearchRoute> { args ->
        SearchRoute(
            initialSearchTerm = args.searchTerm,
            navigateToCategory = navigateToCategory,
            navigateToLogin = navigateToLogin
        )
    }
}

@Composable
private fun SearchRoute(
    initialSearchTerm: String?,
    navigateToCategory: (Category) -> Unit,
    navigateToLogin: () -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            navigateToLogin()
        }
    }

    LaunchedEffect(initialSearchTerm) {
        appActions.setNavArea(NavigationArea.Search)

        var term = initialSearchTerm

        if (term.isNullOrBlank()) {
            term = uiState.activeTerm
        } else {
            vm.search(term)
        }

        appActions.updateTopBar(
            TopBarState().copy(
                showSearch = true,
                initialSearchTerm = term,
            ),
        )
    }

    SearchScreen(
        uiState = uiState,
        onNavigateToCategory = navigateToCategory,
        onContinueSearch = { vm.continueSearch() },
    )
}
