package us.mikeandwan.photos.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.categorylist.CategoryList
import us.mikeandwan.photos.ui.components.filters.NoFilterMatches
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.components.paging.LoadMoreFailed
import us.mikeandwan.photos.ui.components.paging.LoadMoreWhenNearEnd
import us.mikeandwan.photos.ui.shared.toMediaGridItem

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onNavigateToCategory: (Category) -> Unit,
    onToggleFavorite: (Category) -> Unit,
    onContinueSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onSetFilters: (MediaFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.results.isNotEmpty() -> {
                    Results(uiState, onNavigateToCategory, onToggleFavorite, onContinueSearch)
                }

                uiState.isLoading -> {
                    MediaGridSkeleton()
                }

                // the first page failed, which is not the same as the search finding nothing
                uiState.loadFailed -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        LoadMoreFailed(onRetry = onRetrySearch, messageId = R.string.load_failed)
                    }
                }

                uiState.filters.isActive -> {
                    NoFilterMatches(onClear = { onSetFilters(MediaFilters()) })
                }

                else -> {
                    NoResults()
                }
            }
        }

        if (uiState.loadFailed && uiState.results.isNotEmpty()) {
            LoadMoreFailed(onRetry = onRetrySearch)
        }
    }
}

@Composable
private fun NoResults() {
    AsyncImage(
        model = R.drawable.ic_search,
        contentDescription = stringResource(id = R.string.search_icon_description),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .padding(40.dp)
            .fillMaxSize(),
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
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

@Composable
private fun Results(
    uiState: SearchUiState,
    onNavigateToCategory: (Category) -> Unit,
    onToggleFavorite: (Category) -> Unit,
    onContinueSearch: () -> Unit,
) {
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

            MediaGrid(gridState, gridState = lazyGridState)
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
        onRetrySearch = {},
        onSetFilters = {},
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
        onRetrySearch = {},
        onSetFilters = {},
    )
}
