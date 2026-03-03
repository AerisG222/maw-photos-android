package us.mikeandwan.photos.ui.screens.upload

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
object UploadNavKey : NavKey

fun EntryProviderScope<NavKey>.upload() {
    entry<UploadNavKey> {
        UploadRoute()
    }
}

@Composable
private fun UploadRoute(vm: UploadViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            appActions.navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Upload)
        appActions.updateTopBar(
            NavigationArea.Upload,
            TopBarState(
                showAppIcon = false,
                title = "Upload Queue",
            ),
        )
    }

    UploadScreen(uiState = uiState)
}
