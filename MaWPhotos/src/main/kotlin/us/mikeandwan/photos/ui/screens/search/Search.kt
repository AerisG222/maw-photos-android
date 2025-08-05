package us.mikeandwan.photos.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.ui.controls.categorylist.CategoryList
import us.mikeandwan.photos.ui.controls.mediagrid.ImageGrid
import us.mikeandwan.photos.ui.controls.mediagrid.rememberImageGridState
import us.mikeandwan.photos.ui.controls.topbar.TopBarState
import us.mikeandwan.photos.ui.toImageGridItem

@Serializable
data class SearchRoute (
    val searchTerm: String? = null
)

fun NavGraphBuilder.searchScreen(
    navigateToCategory: (Category) -> Unit,
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    navigateToLogin: () -> Unit
) {
    composable<SearchRoute> { backStackEntry ->
        val vm: SearchViewModel = hiltViewModel()
        val args = backStackEntry.toRoute<SearchRoute>()

        val isAuthorized by vm.isAuthorized.collectAsStateWithLifecycle()
        val activeTerm by vm.activeTerm.collectAsStateWithLifecycle()

        LaunchedEffect(isAuthorized) {
            if(!isAuthorized) {
                navigateToLogin()
            }
        }

        LaunchedEffect(args.searchTerm) {
            setNavArea(NavigationArea.Search)

            var term = args.searchTerm

            if(term.isNullOrBlank()) {
                // incoming term is blank, then set to the one in the view model / repository
                // as it will also contain the results from the last query
                term = activeTerm
            } else {
                // for new terms, make sure we execute a search to autopopulate the list
                vm.search(term)
            }

            updateTopBar(
                TopBarState().copy(
                    showSearch = true,
                    initialSearchTerm = term
                )
            )
        }

        val results by vm.searchResultsAsCategories.collectAsStateWithLifecycle()
        val totalFound by vm.totalFound.collectAsStateWithLifecycle()
        val displayType by vm.displayType.collectAsStateWithLifecycle()
        val thumbSize by vm.gridItemThumbnailSize.collectAsStateWithLifecycle()

        SearchScreen(
            results,
            totalFound,
            displayType,
            thumbSize,
            onNavigateToCategory = navigateToCategory,
            continueSearch = { vm.continueSearch() }
        )
    }
}

@Composable
fun SearchScreen(
    results: List<Category>,
    totalFound: Int,
    displayType: CategoryDisplayType,
    thumbSize: GridThumbnailSize,
    onNavigateToCategory: (Category) -> Unit,
    continueSearch: () -> Unit
) {
    if(results.isEmpty()) {
        AsyncImage(
            model = R.drawable.ic_search,
            contentDescription = stringResource(id = R.string.search_icon_description),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(40.dp)
                .fillMaxSize()
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.fragment_search_no_results_found),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontStyle = FontStyle.Italic
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            when (displayType) {
                CategoryDisplayType.Grid -> {
                    val gridState = rememberImageGridState(
                        gridItems = results.map { it.toImageGridItem() },
                        thumbnailSize = thumbSize,
                        onSelectGridItem = { onNavigateToCategory(it.data) }
                    )

                    ImageGrid(gridState)
                }

                CategoryDisplayType.List -> {
                    CategoryList(
                        categories = results,
                        showYear = true,
                        onSelectCategory = onNavigateToCategory
                    )
                }

                else -> {}
            }
        }

        if (results.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if(results.size < totalFound) {
                        Button(onClick = { continueSearch() }) {
                            Text(text = stringResource(id = R.string.fragment_search_load_more))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = results.size.toString())
                        Text(text = "/")
                        Text(text = totalFound.toString())
                    }
                }
            }
        }
    }
}
