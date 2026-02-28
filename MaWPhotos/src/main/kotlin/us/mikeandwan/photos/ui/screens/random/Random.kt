package us.mikeandwan.photos.ui.screens.random

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.controls.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.controls.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.controls.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.controls.topbar.TopBarState
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@Serializable
object RandomRoute : NavKey

fun EntryProviderScope<NavKey>.randomScreen(
    navigateToMedia: (Uuid) -> Unit,
    updateTopBar: (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    navigateToLogin: () -> Unit,
) {
    entry<RandomRoute> {
        val vm: RandomViewModel = hiltViewModel()

        val isAuthorized by vm.isAuthorized.collectAsStateWithLifecycle()
        val photos by vm.media.collectAsStateWithLifecycle()
        val thumbSize by vm.gridItemThumbnailSize.collectAsStateWithLifecycle()

        LaunchedEffect(isAuthorized) {
            if (!isAuthorized) {
                navigateToLogin()
            }
        }

        LaunchedEffect(Unit) {
            vm.initialFetch(24)
        }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            vm.onResume()
        }

        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            vm.onPause()
        }

        RandomScreen(
            photos,
            thumbSize,
            onMediaClicked = { navigateToMedia(it.id) },
            updateTopBar,
            setNavArea,
        )
    }
}

@Composable
fun RandomScreen(
    photos: List<Media>,
    thumbSize: GridThumbnailSize,
    onMediaClicked: (MediaGridItem<Media>) -> Unit,
    updateTopBar: (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        setNavArea(NavigationArea.Random)

        updateTopBar(
            TopBarState().copy(
                title = "Random",
            ),
        )
    }

    val gridState = rememberMediaGridState(
        photos.map { it.toMediaGridItem(thumbSize == GridThumbnailSize.Large) },
        thumbSize,
        onMediaClicked,
    )

    MediaGrid(gridState, modifier = modifier)
}
