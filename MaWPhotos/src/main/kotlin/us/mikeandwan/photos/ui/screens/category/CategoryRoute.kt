package us.mikeandwan.photos.ui.screens.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
data class CategoryRoute(
    val categoryId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.category(
    navigateToMedia: (Media) -> Unit,
    navigateToLogin: () -> Unit,
) {
    entry<CategoryRoute> { args ->
        CategoryRoute(
            categoryId = args.categoryId,
            navigateToMedia = navigateToMedia,
            navigateToLogin = navigateToLogin,
        )
    }
}

@Composable
private fun CategoryRoute(
    categoryId: Uuid,
    navigateToMedia: (Media) -> Unit,
    navigateToLogin: () -> Unit,
    vm: CategoryViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(categoryId) {
        vm.initState(categoryId)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            navigateToLogin()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            appActions.updateTopBar(TopBarState(title = "Loading..."))
        }
    }

    LaunchedEffect(uiState.category) {
        uiState.category?.let {
            appActions.setNavArea(NavigationArea.Category)
            appActions.updateTopBar(TopBarState(title = it.name))
        }
    }

    CategoryScreen(
        uiState = uiState,
        onMediaClicked = navigateToMedia,
    )
}
