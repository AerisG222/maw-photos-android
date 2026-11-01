package us.mikeandwan.photos.ui.screens.categories

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
import us.mikeandwan.photos.ui.components.categorylist.CategoryListSkeleton
import us.mikeandwan.photos.ui.components.filters.NoFilterMatches
import us.mikeandwan.photos.ui.components.filters.ScrollToTopOnChange
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    onRefresh: () -> Unit,
    onNavigateToCategory: (Category) -> Unit,
    onToggleFavorite: (Category) -> Unit,
    onSetFilters: (MediaFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        when (uiState.preferences.displayType) {
            CategoryDisplayType.Grid -> {
                MediaGridSkeleton(modifier = modifier)
            }

            CategoryDisplayType.List -> {
                CategoryListSkeleton(modifier = modifier)
            }
        }

        return
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val lazyGridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    ScrollToTopOnChange(uiState.filters) {
        when (uiState.preferences.displayType) {
            CategoryDisplayType.Grid -> lazyGridState.scrollToItem(0)
            CategoryDisplayType.List -> listState.scrollToItem(0)
        }
    }

    val gridState = rememberMediaGridState(
        gridItems = uiState.categories.map { it.toMediaGridItem() },
        onSelectGridItem = { onNavigateToCategory(it.data) },
        onToggleFavorite = { onToggleFavorite(it.data) },
    )

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        when {
            uiState.categories.isEmpty() -> {
                NoFilterMatches(onClear = { onSetFilters(MediaFilters()) })
            }

            uiState.preferences.displayType == CategoryDisplayType.Grid -> {
                MediaGrid(gridState, gridState = lazyGridState)
            }

            else -> {
                CategoryList(
                    categories = uiState.categories,
                    showYear = false,
                    onSelectCategory = onNavigateToCategory,
                    onToggleFavorite = onToggleFavorite,
                    listState = listState,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    CategoriesScreen(
        uiState = CategoriesUiState(isLoading = false),
        onRefresh = {},
        onNavigateToCategory = {},
        onToggleFavorite = {},
        onSetFilters = {},
    )
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenLoadingPreview() {
    CategoriesScreen(
        uiState = CategoriesUiState(isLoading = true),
        onRefresh = {},
        onNavigateToCategory = {},
        onToggleFavorite = {},
        onSetFilters = {},
    )
}
