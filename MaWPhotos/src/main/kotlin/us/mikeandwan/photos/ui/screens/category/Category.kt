package us.mikeandwan.photos.ui.screens.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.controls.loading.Loading
import us.mikeandwan.photos.ui.controls.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.controls.mediagrid.rememberMediaGridState
import us.mikeandwan.photos.ui.controls.topbar.TopBarState

@Serializable
data class CategoryRoute(
    val categoryId: Uuid,
) : NavKey

fun EntryProviderScope<NavKey>.categoryScreen(
    updateTopBar: (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    navigateToMedia: (Media) -> Unit,
    navigateToLogin: () -> Unit,
) {
    entry<CategoryRoute> { args ->
        val vm: CategoryViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(args.categoryId) {
            vm.initState(args.categoryId)
        }

        when (state) {
            is CategoryState.NotAuthorized -> {
                LaunchedEffect(state) {
                    navigateToLogin()
                }
            }

            is CategoryState.Loading -> {
                LaunchedEffect(Unit) {
                    updateTopBar(
                        TopBarState().copy(
                            title = "",
                        ),
                    )
                }
                Loading()
            }

            is CategoryState.Loaded -> {
                val s = state as CategoryState.Loaded

                LaunchedEffect(s.category) {
                    updateTopBar(
                        TopBarState().copy(
                            title = s.category.name,
                        ),
                    )
                }

                CategoryScreen(
                    s,
                    setNavArea,
                    navigateToMedia,
                )
            }

            is CategoryState.Error -> {} // rely on error snackbar message
        }
    }
}

@Composable
fun CategoryScreen(
    state: CategoryState.Loaded,
    setNavArea: (NavigationArea) -> Unit,
    navigateToMedia: (Media) -> Unit,
) {
    LaunchedEffect(Unit) {
        setNavArea(NavigationArea.Category)
    }

    val gridState = rememberMediaGridState(
        gridItems = state.gridItems,
        thumbnailSize = state.gridItemThumbnailSize,
        onSelectGridItem = { navigateToMedia(it.data) },
    )

    MediaGrid(gridState)
}
