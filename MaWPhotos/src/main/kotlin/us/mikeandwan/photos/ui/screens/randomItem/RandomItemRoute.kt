package us.mikeandwan.photos.ui.screens.randomItem

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
data class RandomItemNavKey(
    val mediaId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.randomItem() {
    entry<RandomItemNavKey> { args ->
        RandomItemRoute(
            mediaId = args.mediaId,
        )
    }
}

@Composable
private fun RandomItemRoute(
    mediaId: Uuid,
    vm: RandomItemViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(mediaId) {
        vm.initState(mediaId)
    }

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Random)
        appActions.updateTopBar(
            NavigationArea.Random,
            TopBarState(title = "Random"),
        )
    }

    RandomItemScreen(
        uiState = uiState,
        videoPlayerDataSourceFactory = vm.videoPlayerDataSourceFactory,
        onNavigateToYear = { appActions.navigateToCategories(it) },
        onNavigateToCategory = { appActions.navigateToCategory(it.id) },
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
