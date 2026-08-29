package us.mikeandwan.photos.ui.screens.faceFeed

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
import us.mikeandwan.photos.ui.components.categorylist.CategoryListSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.shared.toMediaGridItem

// how close to the end of what has been loaded the listing gets before the next page is asked for.
// roughly a screenful at the largest thumbnail size, so the fetch is usually done by the time the
// user scrolls that far.
private const val PAGING_THRESHOLD = 8

@Composable
fun FaceFeedScreen(
    uiState: FaceFeedUiState,
    onMediaClicked: (Media) -> Unit,
    onCategoryClicked: (Category) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    onToggleCategoryFavorite: (Category) -> Unit,
    onSetFavoritesOnly: (Boolean) -> Unit,
    onSetShuffled: (Boolean) -> Unit,
    onSetShowCategories: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FilterBar(
            favoritesOnly = uiState.favoritesOnly,
            isShuffled = uiState.isShuffled,
            showCategories = uiState.showCategories,
            onSetFavoritesOnly = onSetFavoritesOnly,
            onSetShuffled = onSetShuffled,
            onSetShowCategories = onSetShowCategories,
        )

        when {
            uiState.isLoading -> {
                Skeleton(uiState)
            }

            uiState.isEmpty -> {
                Message(emptyMessage(uiState))
            }

            uiState.showCategories -> {
                CategoryListing(
                    uiState = uiState,
                    onCategoryClicked = onCategoryClicked,
                    onToggleFavorite = onToggleCategoryFavorite,
                    onLoadMore = onLoadMore,
                )
            }

            else -> {
                MediaListing(
                    uiState = uiState,
                    onMediaClicked = onMediaClicked,
                    onToggleFavorite = onToggleFavorite,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun MediaListing(
    uiState: FaceFeedUiState,
    onMediaClicked: (Media) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()

    LoadMoreWhenScrolledNearEnd(
        scrollState = gridState,
        lastVisibleIndex = {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        },
        itemCount = uiState.gridItems.size,
        hasMore = uiState.hasMore,
        onLoadMore = onLoadMore,
    )

    val mediaGridState = rememberMediaGridState(
        gridItems = uiState.gridItems,
        thumbnailSize = uiState.thumbnailSize,
        onSelectGridItem = { onMediaClicked(it.data) },
        onToggleFavorite = if (uiState.showFavoriteIndicator) {
            { onToggleFavorite(it.data) }
        } else {
            null
        },
    )

    MediaGrid(mediaGridState, gridState = gridState)
}

/**
 * The categories the subject turns up in, drawn the way the rest of the app draws a list of
 * categories - so whoever has asked for rows rather than a wall of teasers gets them here too.
 */
@Composable
private fun CategoryListing(
    uiState: FaceFeedUiState,
    onCategoryClicked: (Category) -> Unit,
    onToggleFavorite: (Category) -> Unit,
    onLoadMore: () -> Unit,
) {
    val preferences = uiState.categoryPreference

    val toggleFavorite: ((Category) -> Unit)? = when {
        preferences.showFavoriteIndicator -> onToggleFavorite
        else -> null
    }

    when (preferences.displayType) {
        CategoryDisplayType.Grid -> {
            val gridState = rememberLazyGridState()

            LoadMoreWhenScrolledNearEnd(
                scrollState = gridState,
                lastVisibleIndex = {
                    gridState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                },
                itemCount = uiState.categories.size,
                hasMore = uiState.hasMore,
                onLoadMore = onLoadMore,
            )

            val categoryGridState = rememberMediaGridState(
                gridItems = uiState.categories.map {
                    it.toMediaGridItem(
                        useLargeTeaser = preferences.gridThumbnailSize == GridThumbnailSize.Large,
                        showMediaTypeIndicator = preferences.showMediaTypeIndicator,
                    )
                },
                thumbnailSize = preferences.gridThumbnailSize,
                onSelectGridItem = { onCategoryClicked(it.data) },
                onToggleFavorite = toggleFavorite?.let { toggle -> { toggle(it.data) } },
            )

            MediaGrid(categoryGridState, gridState = gridState)
        }

        CategoryDisplayType.List -> {
            val listState = rememberLazyListState()

            LoadMoreWhenScrolledNearEnd(
                scrollState = listState,
                lastVisibleIndex = {
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                },
                itemCount = uiState.categories.size,
                hasMore = uiState.hasMore,
                onLoadMore = onLoadMore,
            )

            CategoryList(
                categories = uiState.categories,
                // a person turns up across years, so the year is what tells two summers apart
                showYear = true,
                onSelectCategory = onCategoryClicked,
                onToggleFavorite = toggleFavorite,
                listState = listState,
            )
        }
    }
}

/**
 * Asks for the next page from how far the listing has been scrolled rather than from its last item
 * composing, so the fetch is already in flight before the user arrives at the end of what is there.
 */
@Composable
private fun LoadMoreWhenScrolledNearEnd(
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

@Composable
private fun Skeleton(uiState: FaceFeedUiState) {
    when {
        uiState.showCategories && uiState.categoryPreference.displayType == CategoryDisplayType.List -> {
            CategoryListSkeleton()
        }

        uiState.showCategories -> {
            MediaGridSkeleton(thumbnailSize = uiState.categoryPreference.gridThumbnailSize)
        }

        else -> {
            MediaGridSkeleton(thumbnailSize = uiState.thumbnailSize)
        }
    }
}

// narrowing to favorites and finding nothing is a real answer about a person the caller can see,
// rather than an empty feed, and is worth saying differently
@Composable
private fun emptyMessage(uiState: FaceFeedUiState): String =
    stringResource(
        id = when {
            uiState.showCategories && uiState.favoritesOnly -> R.string.face_feed_no_favorite_categories
            uiState.showCategories -> R.string.face_feed_no_categories
            uiState.favoritesOnly -> R.string.face_feed_no_favorites
            else -> R.string.face_feed_empty
        },
    )

@Composable
private fun FilterBar(
    favoritesOnly: Boolean,
    isShuffled: Boolean,
    showCategories: Boolean,
    onSetFavoritesOnly: (Boolean) -> Unit,
    onSetShuffled: (Boolean) -> Unit,
    onSetShowCategories: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        // first, because it decides what the toggles after it apply to
        FilterToggle(
            iconId = R.drawable.ic_collections,
            descriptionId = R.string.face_feed_show_categories,
            isActive = showCategories,
            onClick = { onSetShowCategories(!showCategories) },
        )

        // a list of categories has no order to shuffle, and the API takes no seed for one
        if (!showCategories) {
            FilterToggle(
                iconId = R.drawable.ic_shuffle,
                descriptionId = R.string.face_feed_shuffle,
                isActive = isShuffled,
                onClick = { onSetShuffled(!isShuffled) },
            )
        }

        FilterToggle(
            iconId = if (favoritesOnly) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            descriptionId = R.string.face_feed_favorites_only,
            isActive = favoritesOnly,
            onClick = { onSetFavoritesOnly(!favoritesOnly) },
        )
    }
}

@Composable
private fun FilterToggle(
    iconId: Int,
    descriptionId: Int,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = stringResource(id = descriptionId),
            tint = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenLoadingPreview() {
    FaceFeedScreenPreview(FaceFeedUiState(isLoading = true))
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenEmptyPreview() {
    FaceFeedScreenPreview(FaceFeedUiState(isLoading = false, isEmpty = true))
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenNoFavoritesPreview() {
    FaceFeedScreenPreview(FaceFeedUiState(isLoading = false, isEmpty = true, favoritesOnly = true))
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenNoCategoriesPreview() {
    FaceFeedScreenPreview(
        FaceFeedUiState(isLoading = false, isEmpty = true, showCategories = true),
    )
}

@Composable
private fun FaceFeedScreenPreview(uiState: FaceFeedUiState) {
    FaceFeedScreen(
        uiState = uiState,
        onMediaClicked = {},
        onCategoryClicked = {},
        onToggleFavorite = {},
        onToggleCategoryFavorite = {},
        onSetFavoritesOnly = {},
        onSetShuffled = {},
        onSetShowCategories = {},
        onLoadMore = {},
    )
}
