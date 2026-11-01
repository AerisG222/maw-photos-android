package us.mikeandwan.photos.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.components.paging.LoadMoreWhenNearEnd
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onNavigateToCategory: (Category) -> Unit,
    onToggleFavorite: (Category) -> Unit,
    onContinueSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.results.isEmpty()) {
        AsyncImage(
            model = R.drawable.ic_search,
            contentDescription = stringResource(id = R.string.search_icon_description),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = modifier
                .padding(40.dp)
                .fillMaxSize(),
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.fragment_search_no_results_found),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontStyle = FontStyle.Italic,
            )
        }
    }

    when (uiState.displayType) {
        CategoryDisplayType.Grid -> {
            val lazyGridState = rememberLazyGridState()

            LoadMoreWhenNearEnd(
                state = lazyGridState,
                itemCount = uiState.results.size,
                hasMore = uiState.hasMore,
                onLoadMore = onContinueSearch,
            )

            val gridState = rememberMediaGridState(
                gridItems = uiState.results.map { it.toMediaGridItem() },
                onSelectGridItem = { onNavigateToCategory(it.data) },
                onToggleFavorite = { onToggleFavorite(it.data) },
            )

            MediaGrid(gridState, modifier = modifier, gridState = lazyGridState)
        }

        CategoryDisplayType.List -> {
            val listState = rememberLazyListState()

            LoadMoreWhenNearEnd(
                state = listState,
                itemCount = uiState.results.size,
                hasMore = uiState.hasMore,
                onLoadMore = onContinueSearch,
            )

            CategoryList(
                categories = uiState.results,
                showYear = true,
                onSelectCategory = onNavigateToCategory,
                modifier = modifier,
                onToggleFavorite = onToggleFavorite,
                listState = listState,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenNoResultsPreview() {
    SearchScreen(
        uiState = SearchUiState(results = emptyList()),
        onNavigateToCategory = {},
        onToggleFavorite = {},
        onContinueSearch = {},
    )
}

@Preview(showBackground = true)
@Composable
fun SearchScreenResultsPreview() {
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

    SearchScreen(
        uiState = SearchUiState(
            results = listOf(
                Category(
                    id = Uuid.random(),
                    year = 2024,
                    name = "Test Category",
                    effectiveDate = today,
                    modified = now,
                    isFavorite = false,
                    teaser = emptyList(),
                    mediaTypes = listOf(MediaType.Photo),
                ),
            ),
            displayType = CategoryDisplayType.Grid,
        ),
        onNavigateToCategory = {},
        onToggleFavorite = {},
        onContinueSearch = {},
    )
}
