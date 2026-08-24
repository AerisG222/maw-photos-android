package us.mikeandwan.photos.ui.screens.faceFeed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState

// how close to the end of what has been loaded the grid gets before the next page is asked for.
// roughly a screenful at the largest thumbnail size, so the fetch is usually done by the time the
// user scrolls that far.
private const val PAGING_THRESHOLD = 8

@Composable
fun FaceFeedScreen(
    uiState: FaceFeedUiState,
    onMediaClicked: (Media) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    onSetFavoritesOnly: (Boolean) -> Unit,
    onSetShuffled: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FilterBar(
            favoritesOnly = uiState.favoritesOnly,
            isShuffled = uiState.isShuffled,
            onSetFavoritesOnly = onSetFavoritesOnly,
            onSetShuffled = onSetShuffled,
        )

        when {
            uiState.isLoading -> {
                MediaGridSkeleton(thumbnailSize = uiState.thumbnailSize)
            }

            uiState.isEmpty -> {
                val message = if (uiState.favoritesOnly) {
                    // a real answer about a person the caller can see, rather than an empty feed
                    stringResource(id = R.string.face_feed_no_favorites)
                } else {
                    stringResource(id = R.string.face_feed_empty)
                }

                Message(message)
            }

            else -> {
                val gridState = rememberLazyGridState()

                // paging is driven by how far the grid has been scrolled rather than by the last
                // item composing, so the next page is already in flight before the user arrives at
                // the end of this one
                LaunchedEffect(gridState, uiState.gridItems.size, uiState.hasMore) {
                    snapshotFlow {
                        gridState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: 0
                    }.distinctUntilChanged()
                        .filter { lastVisible ->
                            uiState.hasMore && lastVisible >= uiState.gridItems.size - PAGING_THRESHOLD
                        }.collect { onLoadMore() }
                }

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
        }
    }
}

@Composable
private fun FilterBar(
    favoritesOnly: Boolean,
    isShuffled: Boolean,
    onSetFavoritesOnly: (Boolean) -> Unit,
    onSetShuffled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        FilterToggle(
            iconId = R.drawable.ic_shuffle,
            descriptionId = R.string.face_feed_shuffle,
            isActive = isShuffled,
            onClick = { onSetShuffled(!isShuffled) },
        )

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
    FaceFeedScreen(
        uiState = FaceFeedUiState(isLoading = true),
        onMediaClicked = {},
        onToggleFavorite = {},
        onSetFavoritesOnly = {},
        onSetShuffled = {},
        onLoadMore = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenEmptyPreview() {
    FaceFeedScreen(
        uiState = FaceFeedUiState(isLoading = false, isEmpty = true),
        onMediaClicked = {},
        onToggleFavorite = {},
        onSetFavoritesOnly = {},
        onSetShuffled = {},
        onLoadMore = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun FaceFeedScreenNoFavoritesPreview() {
    FaceFeedScreen(
        uiState = FaceFeedUiState(isLoading = false, isEmpty = true, favoritesOnly = true),
        onMediaClicked = {},
        onToggleFavorite = {},
        onSetFavoritesOnly = {},
        onSetShuffled = {},
        onLoadMore = {},
    )
}
