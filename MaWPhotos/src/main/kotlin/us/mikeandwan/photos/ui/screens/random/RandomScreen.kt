package us.mikeandwan.photos.ui.screens.random

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.components.paging.LoadMoreWhenNearEnd
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@Composable
fun RandomScreen(
    uiState: RandomUiState,
    onMediaClicked: (MediaGridItem<Media>) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyGridState = rememberLazyGridState()

    // random media never runs out, so there is always more to ask for
    LoadMoreWhenNearEnd(
        state = lazyGridState,
        itemCount = uiState.media.size,
        hasMore = true,
        onLoadMore = onLoadMore,
    )

    val gridState = rememberMediaGridState(
        uiState.media.map { it.toMediaGridItem() },
        onMediaClicked,
        onToggleFavorite = { onToggleFavorite(it.data) },
    )

    MediaGrid(gridState, modifier = modifier, gridState = lazyGridState)
}

@Preview(showBackground = true)
@Composable
fun RandomScreenPreview() {
    RandomScreen(
        uiState = RandomUiState(),
        onMediaClicked = {},
        onToggleFavorite = {},
        onLoadMore = {},
    )
}
