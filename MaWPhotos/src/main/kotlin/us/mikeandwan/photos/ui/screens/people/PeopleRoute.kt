package us.mikeandwan.photos.ui.screens.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
object PeopleNavKey : NavKey

fun EntryProviderScope<NavKey>.people() {
    entry<PeopleNavKey> {
        PeopleRoute()
    }
}

@Composable
private fun PeopleRoute(vm: PeopleViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val title = stringResource(id = R.string.people_title)

    LaunchedEffect(title) {
        appActions.setNavArea(NavigationArea.People)
        appActions.updateTopBar(
            NavigationArea.People,
            TopBarState(title = title),
        )
    }

    PeopleScreen(
        uiState = uiState,
        onFilterChange = { vm.setFilter(it) },
        onToggleSort = { vm.toggleSort() },
        onToggleFavorite = { vm.toggleFavorite(it) },
        // the person feed arrives with the next set of screens; until then a person is somebody to
        // find and mark rather than open, so the cards stay inert
        onSelectPerson = null,
    )
}
