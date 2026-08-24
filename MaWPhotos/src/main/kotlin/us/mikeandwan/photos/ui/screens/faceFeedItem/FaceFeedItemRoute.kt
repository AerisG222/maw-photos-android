package us.mikeandwan.photos.ui.screens.faceFeedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

@Serializable
data class PersonMediaItemNavKey(
    val personId: Uuid,
    val mediaId: Uuid,
) : NavKey

@Serializable
data class ClanMediaItemNavKey(
    val clanId: Uuid,
    val mediaId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.faceFeedItem() {
    entry<PersonMediaItemNavKey> { args ->
        FaceFeedItemRoute(
            subject = FaceFeedSubject.Person(args.personId),
            mediaId = args.mediaId,
        )
    }

    entry<ClanMediaItemNavKey> { args ->
        FaceFeedItemRoute(
            subject = FaceFeedSubject.Clan(args.clanId),
            mediaId = args.mediaId,
        )
    }
}

@Composable
private fun FaceFeedItemRoute(
    subject: FaceFeedSubject,
    mediaId: Uuid,
    vm: FaceFeedItemViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    DisposableEffect(Unit) {
        onDispose { vm.reset() }
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.People)
    }

    LaunchedEffect(subject, mediaId) {
        vm.initState(subject, mediaId)
    }

    // a face feed spans categories, so the title follows the media rather than the subject - which
    // is the same thing the random feed does, and for the same reason
    LaunchedEffect(uiState.category) {
        uiState.category?.let {
            appActions.updateTopBar(
                NavigationArea.People,
                TopBarState(
                    title = it.name,
                    tinyVerticalTitlePrefix = it.year.toString(),
                ),
            )
        }
    }

    FaceFeedItemScreen(
        uiState = uiState,
        videoPlayerDataSourceFactory = vm.videoPlayerDataSourceFactory,
        onSetActiveId = { vm.setActiveId(it) },
        onToggleSlideshow = { vm.toggleSlideshow() },
        onToggleFavorite = { vm.toggleFavorite() },
        onToggleDetails = { vm.toggleShowDetails() },
        onFetchExif = { vm.fetchExif() },
        onFetchComments = { vm.fetchCommentDetails() },
        onAddComment = { vm.addComment(it) },
        onSaveMediaToShare = { drawable, filename, onComplete ->
            vm.saveFileToShare(drawable, filename, onComplete)
        },
    )
}
