package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

@Immutable
data class MediaGridState<T>(
    val gridItems: List<MediaGridItem<T>>,
    val onSelectGridItem: (MediaGridItem<T>) -> Unit,
    // when null, items do not show the favorite badge - which is for the grids where there is
    // nothing to mark, not for a preference: the badge itself is always on offer
    val onToggleFavorite: ((MediaGridItem<T>) -> Unit)? = null,
)

@Composable
fun <T> rememberMediaGridState(
    gridItems: List<MediaGridItem<T>> = emptyList(),
    onSelectGridItem: (MediaGridItem<T>) -> Unit = {},
    onToggleFavorite: ((MediaGridItem<T>) -> Unit)? = null,
): MediaGridState<T> =
    remember(gridItems, onSelectGridItem, onToggleFavorite) {
        MediaGridState(
            gridItems,
            onSelectGridItem,
            onToggleFavorite,
        )
    }
