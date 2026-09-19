package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// Stable selectors for UI automation (baseline profile generation). Surfaced to UiAutomator via
// `testTagsAsResourceId` enabled at the app root. Keep in sync with the matching literals in the
// :baselineprofile module's BaselineProfileGenerator.
const val MEDIA_GRID_TAG = "mediaGrid"
const val MEDIA_GRID_ITEM_TAG = "mediaGridItem"

// One size everywhere, rather than a density setting per area.  The grid is adaptive, so this is
// the narrowest a column may be before another one is dropped - on a phone that lands at three or
// four across, which is what the old medium default gave and what the web app settled on.
val MEDIA_GRID_ITEM_SIZE = 120.dp

@Composable
fun <T> MediaGrid(
    state: MediaGridState<T>,
    modifier: Modifier = Modifier,
    // hoisted for the callers that need to watch it - a paged feed asks for the next page from how
    // far it has been scrolled
    gridState: LazyGridState = rememberLazyGridState(),
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = MEDIA_GRID_ITEM_SIZE),
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
                size = MEDIA_GRID_ITEM_SIZE,
                onSelectImage = { item -> state.onSelectGridItem(item) },
                onToggleFavorite = state.onToggleFavorite,
                modifier = Modifier
                    .testTag(MEDIA_GRID_ITEM_TAG)
                    .animateItem(),
            )
        }
    }
}
