package us.mikeandwan.photos.ui.screens.categories

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
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
