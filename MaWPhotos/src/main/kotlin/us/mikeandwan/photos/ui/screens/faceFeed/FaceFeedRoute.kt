package us.mikeandwan.photos.ui.screens.faceFeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

/*
   What one person appears in, and what anyone in a clan appears in - the media itself, or the
   categories holding it.

   Two keys and one screen: the two feeds differ only in which endpoint answers, so everything the
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

fun EntryProviderScope<NavKey>.faceFeed() {
    entry<PersonMediaNavKey> { args ->
        FaceFeedRoute(subject = FaceFeedSubject.Person(args.personId))
    }

    entry<ClanMediaNavKey> { args ->
        FaceFeedRoute(subject = FaceFeedSubject.Clan(args.clanId))
    }
}

@Composable
private fun FaceFeedRoute(
    subject: FaceFeedSubject,
    vm: FaceFeedViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    // browsing by person stays lit in the rail while inside one of its feeds - a person or a clan is
    // a way into the same library rather than a separate place
    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.People)
    }

    // lights up this person or clan in the rail, where the rest of them are listed to switch to
    LaunchedEffect(subject) {
        appActions.setActiveFaceSubject(subject)
    }

    LaunchedEffect(subject) {
        vm.initState(subject)
    }

    LaunchedEffect(uiState.title) {
        appActions.updateTopBar(
            NavigationArea.People,
            TopBarState(title = uiState.title),
        )
    }

    FaceFeedScreen(
        uiState = uiState,
        onMediaClicked = { media -> appActions.navigateToFaceFeedItem(subject, media.id) },
        // a category is a way back into the library proper rather than another face feed, so it
        // opens where every other category does
        onCategoryClicked = { category -> appActions.navigateToCategory(category.id) },
        onToggleFavorite = { vm.toggleFavorite(it) },
        onToggleCategoryFavorite = { vm.toggleCategoryFavorite(it) },
        onSetFavoritesOnly = { vm.setFavoritesOnly(it) },
        onSetShuffled = { vm.setShuffled(it) },
        onSetShowCategories = { vm.setShowCategories(it) },
        onLoadMore = { vm.loadNextPage() },
    )
}
