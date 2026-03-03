package us.mikeandwan.photos.ui.screens.about

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
object AboutNavKey : NavKey

fun EntryProviderScope<NavKey>.about() {
    entry<AboutNavKey> {
        AboutRoute()
    }
}

@Composable
private fun AboutRoute(vm: AboutViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.About)
        appActions.updateTopBar(
            NavigationArea.About,
            TopBarState(
                showAppIcon = false,
                title = "About",
            ),
        )
    }

    AboutScreen(uiState)
}
