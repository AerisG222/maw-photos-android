package us.mikeandwan.photos.ui.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
object LoginNavKey : NavKey

fun EntryProviderScope<NavKey>.login() {
    entry<LoginNavKey> {
        LoginRoute()
    }
}

@Composable
private fun LoginRoute(vm: LoginViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val context = LocalContext.current

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            appActions.navigateToCategories(null)
        }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Login)
        appActions.updateTopBar(
            NavigationArea.Login,
            TopBarState(show = false),
        )
    }

    LoginScreen(
        uiState = uiState,
        onLogin = { vm.login(context) },
    )
}
