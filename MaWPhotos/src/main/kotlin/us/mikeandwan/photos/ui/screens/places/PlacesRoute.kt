package us.mikeandwan.photos.ui.screens.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

/**
 * One level of the place tree.
 *
 * The place being looked at is in the key rather than in the view model, so drilling in pushes a
 * level onto the back stack and Back walks out the way the user came - which is the same shape the
 * categories area uses for a year. Null lists the countries, which is the root, and is the key the
 * navigation rail's entry points at.
 */
@Serializable
data class PlacesNavKey(
    val placeId: Uuid? = null,
) : NavKey

fun EntryProviderScope<NavKey>.places() {
    entry<PlacesNavKey> { args ->
        PlacesRoute(placeId = args.placeId)
    }
}

@Composable
private fun PlacesRoute(
    placeId: Uuid?,
    vm: PlacesViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val knownPlaces by vm.knownPlaces.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val rootTitle = stringResource(id = R.string.places_title)

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Place)
        // the tree itself is nowhere in particular, so nothing in the rail's list is current
        appActions.setActiveFeedSubject(null)
    }

    LaunchedEffect(placeId) {
        vm.setPlace(placeId)
    }

    // named by where you are rather than by the area: the breadcrumb already says how you got
    // here, and the bar is the one place with room for the name of the level itself
    LaunchedEffect(uiState.place, rootTitle) {
        appActions.updateTopBar(
            NavigationArea.Place,
            TopBarState(title = uiState.place?.name ?: rootTitle),
        )
    }

    PlacesScreen(
        uiState = uiState,
        knownPlaces = knownPlaces,
        // a tile leads to whatever is actually inside it: at the bottom of the tree that is the
        // photographs, so a city opens its feed rather than a level that would only say there is
        // nothing further down
        onSelectPlace = { place ->
            if (place.isLeaf) {
                appActions.navigateToMediaFeed(MediaFeedSubject.Place(place.id))
            } else {
                appActions.navigateToPlace(place.id)
            }
        },
        onSelectChainLink = { appActions.navigateToPlace(it) },
        onViewMedia = { appActions.navigateToMediaFeed(MediaFeedSubject.Place(it)) },
    )
}
