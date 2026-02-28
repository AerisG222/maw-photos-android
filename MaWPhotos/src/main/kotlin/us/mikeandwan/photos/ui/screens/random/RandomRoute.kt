package us.mikeandwan.photos.ui.screens.random

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
object RandomRoute : NavKey

fun EntryProviderScope<NavKey>.random(
    navigateToMedia: (Uuid) -> Unit,
    navigateToLogin: () -> Unit,
) {
    entry<RandomRoute> {
        RandomRoute(
            navigateToMedia = navigateToMedia,
            navigateToLogin = navigateToLogin,
        )
    }
}

@Composable
private fun RandomRoute(
    navigateToMedia: (Uuid) -> Unit,
    navigateToLogin: () -> Unit,
    vm: RandomViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        vm.initialFetch(24)
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Random)
        appActions.updateTopBar(TopBarState(title = "Random"))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.onResume()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        vm.onPause()
    }

    RandomScreen(
        uiState = uiState,
        onMediaClicked = { navigateToMedia(it.id) },
    )
}
