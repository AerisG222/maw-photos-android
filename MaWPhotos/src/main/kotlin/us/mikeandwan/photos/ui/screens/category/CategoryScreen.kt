package us.mikeandwan.photos.ui.screens.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState

@Composable
fun CategoryScreen(
    uiState: CategoryUiState,
    onMediaClicked: (Media) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        MediaGridSkeleton(
            thumbnailSize = uiState.gridItemThumbnailSize,
            modifier = modifier,
        )

        return
    }

    val gridState = rememberMediaGridState(
        gridItems = uiState.gridItems,
        thumbnailSize = uiState.gridItemThumbnailSize,
        onSelectGridItem = { onMediaClicked(it.data) },
        onToggleFavorite = if (uiState.showFavoriteIndicator) {
            { onToggleFavorite(it.data) }
        } else {
            null
        },
    )

    MediaGrid(gridState, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenPreview() {
    CategoryScreen(
        uiState = CategoryUiState(isLoading = false),
        onMediaClicked = {},
        onToggleFavorite = {},
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenLoadingPreview() {
    CategoryScreen(
        uiState = CategoryUiState(isLoading = true),
        onMediaClicked = {},
        onToggleFavorite = {},
    )
}
