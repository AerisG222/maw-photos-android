package us.mikeandwan.photos.ui.components.paging

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// how close to the end of what has been loaded the listing gets before the next page is asked for.
// roughly a screenful at the largest thumbnail size, so the fetch is usually done by the time the
// user scrolls that far.
private const val PAGING_THRESHOLD = 8

@Composable
fun LoadMoreWhenNearEnd(
    state: LazyGridState,
    itemCount: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LoadMoreWhenNearEnd(
        scrollState = state,
        lastVisibleIndex = {
            state.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        },
        itemCount = itemCount,
        hasMore = hasMore,
        onLoadMore = onLoadMore,
    )
}

@Composable
fun LoadMoreWhenNearEnd(
    state: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LoadMoreWhenNearEnd(
        scrollState = state,
        lastVisibleIndex = {
            state.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        },
        itemCount = itemCount,
        hasMore = hasMore,
        onLoadMore = onLoadMore,
    )
}

/**
 * Asks for the next page from how far the listing has been scrolled rather than from its last item
 * composing, so the fetch is already in flight before the user arrives at the end of what is there.
 *
 * The check runs again whenever the item count changes, so a page too short to fill the screen
 * leads straight to the next one. Whatever answers [onLoadMore] has to guard against being asked
 * twice: this can ask again before an earlier request lands.
 */
@Composable
private fun LoadMoreWhenNearEnd(
    scrollState: ScrollableState,
    lastVisibleIndex: () -> Int,
    itemCount: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(scrollState, itemCount, hasMore) {
        snapshotFlow(lastVisibleIndex)
            .distinctUntilChanged()
            .filter { lastVisible -> hasMore && lastVisible >= itemCount - PAGING_THRESHOLD }
            .collect { onLoadMore() }
    }
}
