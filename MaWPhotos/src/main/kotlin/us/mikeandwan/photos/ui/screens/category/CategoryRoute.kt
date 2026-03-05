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
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
data class CategoryNavKey(
    val categoryId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.category() {
    entry<CategoryNavKey> { args ->
        CategoryRoute(
            categoryId = args.categoryId,
        )
    }
}

@Composable
private fun CategoryRoute(
    categoryId: Uuid,
    vm: CategoryViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Category)
    }

    LaunchedEffect(categoryId) {
        vm.initState(categoryId)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            appActions.updateTopBar(
                NavigationArea.Category,
                TopBarState(title = "Loading..."),
            )
        }
    }

    LaunchedEffect(uiState.category) {
        uiState.category?.let {
            appActions.updateTopBar(
                NavigationArea.Category,
                TopBarState(
                    tinyVerticalTitlePrefix = it.year.toString(),
                    title = it.name,
                ),
            )
        }
    }

    CategoryScreen(
        uiState = uiState,
        onMediaClicked = { media ->
            appActions.navigateToCategoryItem(media.categoryId, media.id)
        },
    )
}
