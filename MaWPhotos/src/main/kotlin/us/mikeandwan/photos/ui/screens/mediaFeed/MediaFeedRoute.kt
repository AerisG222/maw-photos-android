package us.mikeandwan.photos.ui.screens.mediaFeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

/*
   What one person appears in, what anyone in a clan appears in, and what was taken at one place -
   the media itself, or the categories holding it.

   Three keys and one screen: the feeds differ only in which endpoint answers, so everything the
   user can see - paging, the filters, the shuffle, the pager behind a tap - is identical by
   construction rather than by maintenance.
*/
@Serializable
data class PersonMediaNavKey(
    val personId: Uuid,
) : NavKey

@Serializable
data class ClanMediaNavKey(
    val clanId: Uuid,
) : NavKey

@Serializable
data class PlaceMediaNavKey(
    val placeId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.mediaFeed() {
    entry<PersonMediaNavKey> { args ->
        MediaFeedRoute(subject = MediaFeedSubject.Person(args.personId))
    }

    entry<ClanMediaNavKey> { args ->
        MediaFeedRoute(subject = MediaFeedSubject.Clan(args.clanId))
    }

    entry<PlaceMediaNavKey> { args ->
        MediaFeedRoute(subject = MediaFeedSubject.Place(args.placeId))
    }
}

@Composable
private fun MediaFeedRoute(
    subject: MediaFeedSubject,
    vm: MediaFeedViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val knownPlaces by vm.knownPlaces.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    // the area the feed was opened from stays lit in the rail while inside it - a person, a clan or
    // a place is a way into the same library rather than a separate area of the app
    LaunchedEffect(subject) {
        appActions.setNavArea(subject.navigationArea)
    }

    // lights up this subject in the rail, where the rest of its kind are listed to switch to
    LaunchedEffect(subject) {
        appActions.setActiveFeedSubject(subject)
    }

    LaunchedEffect(subject) {
        vm.initState(subject)
    }

    LaunchedEffect(uiState.title, subject) {
        appActions.updateTopBar(
            subject.navigationArea,
            TopBarState(title = uiState.title),
        )
    }

    MediaFeedScreen(
        uiState = uiState,
        knownPlaces = knownPlaces,
        // a rung is a level of the tree rather than another feed, so it leads back to the browse -
        // and to the level it names, unwinding the drill-down rather than stacking onto it
        onSelectPlace = { appActions.navigateToPlace(it) },
        onMediaClicked = { media -> appActions.navigateToMediaFeedItem(subject, media.id) },
        // a category is a way back into the library proper rather than another media feed, so it
        // opens where every other category does
        onCategoryClicked = { category -> appActions.navigateToCategory(category.id) },
        onToggleFavorite = { vm.toggleFavorite(it) },
        onToggleCategoryFavorite = { vm.toggleCategoryFavorite(it) },
        onSetFavoritesOnly = { vm.setFavoritesOnly(it) },
        onSetShuffled = { vm.setShuffled(it) },
        onSetShowCategories = { vm.setShowCategories(it) },
        onSetShowCategoryYear = { vm.setShowCategoryYear(it) },
        onSetShowCategoryTitle = { vm.setShowCategoryTitle(it) },
        onLoadMore = { vm.loadNextPage() },
    )
}
