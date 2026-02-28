package us.mikeandwan.photos.ui.screens.inactiveUser

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
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
object InactiveUserRoute : NavKey

fun EntryProviderScope<NavKey>.inactiveUser(
    navigateToLogin: () -> Unit,
    navigateAfterActivated: () -> Unit,
) {
    entry<InactiveUserRoute> {
        InactiveUserRoute(
            navigateToLogin = navigateToLogin,
            navigateAfterActivated = navigateAfterActivated
        )
    }
}

@Composable
private fun InactiveUserRoute(
    navigateToLogin: () -> Unit,
    navigateAfterActivated: () -> Unit,
    vm: InactiveUserViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val context = LocalContext.current

    LaunchedEffect(uiState.userStatus) {
        if (uiState.userStatus is UserStatus.Active) {
            navigateAfterActivated()
        }
    }

    LaunchedEffect(Unit) {
        appActions.updateTopBar(TopBarState(show = false))
        appActions.setNavArea(NavigationArea.Login)
    }

    InactiveUserScreen(
        uiState = uiState,
        onRequeryStatus = { vm.queryUserStatus() },
        onLogout = {
            vm.logout(context)
            navigateToLogin()
        }
    )
}
