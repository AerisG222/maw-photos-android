package us.mikeandwan.photos.ui.screens.categories

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    onRefresh: () -> Unit,
    onNavigateToCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Loading()
        return
    }

    val pullToRefreshState = rememberPullToRefreshState()

    val gridState = rememberMediaGridState(
        gridItems = uiState.categories.map {
            it.toMediaGridItem(
                uiState.preferences.gridThumbnailSize == GridThumbnailSize.Large,
            )
        },
        thumbnailSize = uiState.preferences.gridThumbnailSize,
        onSelectGridItem = { onNavigateToCategory(it.data) },
    )

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        when (uiState.preferences.displayType) {
            CategoryDisplayType.Grid -> {
                MediaGrid(gridState)
            }

            CategoryDisplayType.List -> {
                CategoryList(
                    categories = uiState.categories,
                    showYear = false,
                    onSelectCategory = onNavigateToCategory,
                )
            }

            else -> {}
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    CategoriesScreen(
        uiState = CategoriesUiState(isLoading = false),
        onRefresh = {},
        onNavigateToCategory = {}
    )
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenLoadingPreview() {
    CategoriesScreen(
        uiState = CategoriesUiState(isLoading = true),
        onRefresh = {},
        onNavigateToCategory = {}
    )
}
