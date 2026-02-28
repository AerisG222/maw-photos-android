package us.mikeandwan.photos.ui.screens.categories

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
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
data class CategoriesRoute(
    val year: Int?,
) : NavKey

fun EntryProviderScope<NavKey>.categories(
    navigateToCategory: (Category) -> Unit,
    setActiveYear: (Int) -> Unit,
    navigateToLogin: () -> Unit,
    navigateToCategories: (Int) -> Unit,
) {
    entry<CategoriesRoute> { args ->
        CategoriesRoute(
            initialYear = args.year,
            navigateToCategory = navigateToCategory,
            setActiveYear = setActiveYear,
            navigateToLogin = navigateToLogin,
            navigateToCategories = navigateToCategories,
        )
    }
}

@Composable
private fun CategoriesRoute(
    initialYear: Int?,
    navigateToCategory: (Category) -> Unit,
    setActiveYear: (Int) -> Unit,
    navigateToLogin: () -> Unit,
    navigateToCategories: (Int) -> Unit,
    vm: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(initialYear) {
        vm.setYear(initialYear)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            navigateToLogin()
        }
    }

    LaunchedEffect(uiState.invalidYearMostRecent) {
        uiState.invalidYearMostRecent?.let {
            navigateToCategories(it)
        }
    }

    LaunchedEffect(uiState.year) {
        uiState.year?.let {
            setActiveYear(it)
            appActions.setNavArea(NavigationArea.Category)
            appActions.updateTopBar(TopBarState(title = it.toString()))
        }
    }

    CategoriesScreen(
        uiState = uiState,
        onRefresh = { vm.refreshCategories() },
        onNavigateToCategory = navigateToCategory,
    )
}
