package us.mikeandwan.photos.ui.screens.categories

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
data class CategoriesNavKey(
    val year: Int?,
) : NavKey

fun EntryProviderScope<NavKey>.categories() {
    entry<CategoriesNavKey> { args ->
        CategoriesRoute(
            initialYear = args.year,
        )
    }
}

@Composable
private fun CategoriesRoute(
    initialYear: Int?,
    vm: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Category)
    }

    LaunchedEffect(initialYear) {
        vm.setYear(initialYear)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(uiState.invalidYearMostRecent) {
        uiState.invalidYearMostRecent?.let {
            appActions.navigateToCategories(it)
        }
    }

    LaunchedEffect(uiState.year) {
        uiState.year?.let {
            appActions.setActiveYear(it)
            appActions.updateTopBar(
                NavigationArea.Category,
                TopBarState(title = it.toString()),
            )
        }
    }

    CategoriesScreen(
        uiState = uiState,
        onRefresh = { vm.refreshCategories() },
        onNavigateToCategory = { appActions.navigateToCategory(it.id) },
    )
}
