package us.mikeandwan.photos.ui.screens.random

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
object RandomNavKey : NavKey

fun EntryProviderScope<NavKey>.random() {
    entry<RandomNavKey> {
        RandomRoute()
    }
}

@Composable
private fun RandomRoute(vm: RandomViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Random)
        appActions.updateTopBar(
            NavigationArea.Random,
            TopBarState(title = "Random"),
        )
    }

    RandomScreen(
        uiState = uiState,
        onMediaClicked = { appActions.navigateToRandomItem(it.id) },
        onToggleFavorite = { media -> vm.toggleFavorite(media) },
        onLoadMore = { vm.loadNextPage() },
        onRetryLoad = { vm.retryLoad() },
    )
}
