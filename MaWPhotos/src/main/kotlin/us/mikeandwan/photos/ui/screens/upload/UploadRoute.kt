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
object UploadRoute : NavKey

fun EntryProviderScope<NavKey>.upload(
    navigateToLogin: () -> Unit,
) {
    entry<UploadRoute> {
        UploadRoute(navigateToLogin = navigateToLogin)
    }
}

@Composable
private fun UploadRoute(
    navigateToLogin: () -> Unit,
    vm: UploadViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(uiState.isAuthorized) {
        if (!uiState.isAuthorized) {
            navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Upload)
        appActions.updateTopBar(
            TopBarState(
                showAppIcon = false,
                title = "Upload Queue",
            )
        )
    }

    UploadScreen(uiState = uiState)
}
