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
import us.mikeandwan.photos.ui.controls.topbar.TopBarState

@Serializable
object AboutRoute : NavKey

fun EntryProviderScope<NavKey>.about(
    updateTopBar: (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
) {
    entry<AboutRoute> {
        AboutRoute(
            updateTopBar = updateTopBar,
            setNavArea = setNavArea,
        )
    }
}

@Composable
private fun AboutRoute(
    updateTopBar: (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    vm: AboutViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        setNavArea(NavigationArea.About)
        updateTopBar(
            TopBarState().copy(
                showAppIcon = false,
                title = "About",
            ),
        )
    }

    AboutScreen(uiState)
}
