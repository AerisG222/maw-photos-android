package us.mikeandwan.photos.ui.screens.random

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@Composable
fun RandomScreen(
    uiState: RandomUiState,
    onMediaClicked: (MediaGridItem<Media>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberMediaGridState(
        uiState.media.map { it.toMediaGridItem(uiState.thumbnailSize == GridThumbnailSize.Large) },
        uiState.thumbnailSize,
        onMediaClicked,
    )

    MediaGrid(gridState, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun RandomScreenPreview() {
    RandomScreen(
        uiState = RandomUiState(isAuthorized = true),
        onMediaClicked = {}
    )
}
