package us.mikeandwan.photos.ui.screens.categoryItem

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
data class CategoryItemNavKey(
    val categoryId: Uuid,
    val mediaId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.categoryItem() {
    entry<CategoryItemNavKey> { args ->
        CategoryItemRoute(
            categoryId = args.categoryId,
            mediaId = args.mediaId,
        )
    }
}

@Composable
private fun CategoryItemRoute(
    categoryId: Uuid,
    mediaId: Uuid,
    vm: CategoryItemViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Category)
    }

    LaunchedEffect(categoryId, mediaId) {
        vm.initState(categoryId, mediaId)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(uiState.category) {
        uiState.category?.let {
            appActions.updateTopBar(
                NavigationArea.Category,
                TopBarState(title = it.name),
            )
        }
    }

    CategoryItemScreen(
        uiState = uiState,
        videoPlayerDataSourceFactory = vm.videoPlayerDataSourceFactory,
        onSetActiveIndex = { vm.setActiveIndex(it) },
        onToggleSlideshow = { vm.toggleSlideshow() },
        onToggleFavorite = { vm.toggleFavorite() },
        onToggleDetails = { vm.toggleShowDetails() },
        onFetchExif = { vm.fetchExif() },
        onFetchComments = { vm.fetchCommentDetails() },
        onAddComment = { vm.addComment(it) },
        onSaveMediaToShare = { drawable, filename, onComplete ->
            vm.saveFileToShare(drawable, filename, onComplete)
        },
    )
}
