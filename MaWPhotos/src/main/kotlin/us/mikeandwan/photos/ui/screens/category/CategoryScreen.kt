package us.mikeandwan.photos.ui.screens.category

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.ui.components.filters.NoFilterMatches
import us.mikeandwan.photos.ui.components.filters.ScrollToTopOnChange
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState

@Composable
fun CategoryScreen(
    uiState: CategoryUiState,
    onMediaClicked: (Media) -> Unit,
    onToggleFavorite: (Media) -> Unit,
    onSetFilters: (MediaFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        MediaGridSkeleton(modifier = modifier)

        return
    }

    val gridState = rememberMediaGridState(
        gridItems = uiState.gridItems,
        onSelectGridItem = { onMediaClicked(it.data) },
        onToggleFavorite = { onToggleFavorite(it.data) },
    )

    val lazyGridState = rememberLazyGridState()

    ScrollToTopOnChange(uiState.filters) {
        lazyGridState.scrollToItem(0)
    }

    if (uiState.gridItems.isEmpty() && uiState.filters.isActive) {
        NoFilterMatches(onClear = { onSetFilters(MediaFilters()) }, modifier = modifier)
    } else {
        MediaGrid(gridState, modifier = modifier, gridState = lazyGridState)
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenPreview() {
    CategoryScreen(
        uiState = CategoryUiState(isLoading = false),
        onMediaClicked = {},
        onToggleFavorite = {},
        onSetFilters = {},
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenLoadingPreview() {
    CategoryScreen(
        uiState = CategoryUiState(isLoading = true),
        onMediaClicked = {},
        onToggleFavorite = {},
        onSetFilters = {},
    )
}
