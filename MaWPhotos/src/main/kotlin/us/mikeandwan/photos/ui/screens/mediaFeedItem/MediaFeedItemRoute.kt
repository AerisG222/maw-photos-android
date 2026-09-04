package us.mikeandwan.photos.ui.screens.mediaFeedItem

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
import us.mikeandwan.photos.domain.models.MediaFeedSubject
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

@Serializable
data class PlaceMediaItemNavKey(
    val placeId: Uuid,
    val mediaId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.mediaFeedItem() {
    entry<PersonMediaItemNavKey> { args ->
        MediaFeedItemRoute(
            subject = MediaFeedSubject.Person(args.personId),
            mediaId = args.mediaId,
        )
    }

    entry<ClanMediaItemNavKey> { args ->
        MediaFeedItemRoute(
            subject = MediaFeedSubject.Clan(args.clanId),
            mediaId = args.mediaId,
        )
    }

    entry<PlaceMediaItemNavKey> { args ->
        MediaFeedItemRoute(
            subject = MediaFeedSubject.Place(args.placeId),
            mediaId = args.mediaId,
        )
    }
}

@Composable
private fun MediaFeedItemRoute(
    subject: MediaFeedSubject,
    mediaId: Uuid,
    vm: MediaFeedItemViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current

    DisposableEffect(Unit) {
        onDispose { vm.reset() }
    }

    LaunchedEffect(subject) {
        appActions.setNavArea(subject.navigationArea)
    }

    LaunchedEffect(subject) {
        appActions.setActiveFeedSubject(subject)
    }

    LaunchedEffect(subject, mediaId) {
        vm.initState(subject, mediaId)
    }

    // a media feed spans categories, so the title follows the media rather than the subject - which
    // is the same thing the random feed does, and for the same reason
    LaunchedEffect(uiState.category, subject) {
        uiState.category?.let {
            appActions.updateTopBar(
                subject.navigationArea,
                TopBarState(
                    title = it.name,
                    tinyVerticalTitlePrefix = it.year.toString(),
                ),
            )
        }
    }

    MediaFeedItemScreen(
        uiState = uiState,
        videoPlayerDataSourceFactory = vm.videoPlayerDataSourceFactory,
        onSetActiveId = { vm.setActiveId(it) },
        onToggleSlideshow = { vm.toggleSlideshow() },
        onToggleFavorite = { vm.toggleFavorite() },
        onToggleFaceHighlights = { vm.toggleFaceHighlights() },
        onToggleDetails = { vm.toggleShowDetails() },
        onFetchExif = { vm.fetchExif() },
        onFetchComments = { vm.fetchCommentDetails() },
        onAddComment = { vm.addComment(it) },
        onSaveMediaToShare = { drawable, filename, onComplete ->
            vm.saveFileToShare(drawable, filename, onComplete)
        },
    )
}
