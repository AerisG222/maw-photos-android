package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.domain.models.GridThumbnailSize

// Stable selectors for UI automation (baseline profile generation). Surfaced to UiAutomator via
// `testTagsAsResourceId` enabled at the app root. Keep in sync with the matching literals in the
// :baselineprofile module's BaselineProfileGenerator.
const val MEDIA_GRID_TAG = "mediaGrid"
const val MEDIA_GRID_ITEM_TAG = "mediaGridItem"

@Composable
fun <T> MediaGrid(
    state: MediaGridState<T>,
    modifier: Modifier = Modifier,
) {
    if (state.size > 0.dp) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = state.size),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = modifier.testTag(MEDIA_GRID_TAG),
        ) {
            items(
                state.gridItems,
                key = { item -> item.id },
            ) {
                MediaGridImage(
                    item = it,
                    size = state.size,
                    onSelectImage = { item -> state.onSelectGridItem(item) },
                    modifier = Modifier.testTag(MEDIA_GRID_ITEM_TAG),
                )
            }
        }
    }
}

internal fun getSize(size: GridThumbnailSize): Dp =
    when (size) {
        GridThumbnailSize.ExtraSmall -> 60.dp
        GridThumbnailSize.Small -> 90.dp
        GridThumbnailSize.Medium -> 120.dp
        GridThumbnailSize.Large -> 180.dp
        GridThumbnailSize.Unspecified -> 0.dp
    }
