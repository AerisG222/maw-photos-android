package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import us.mikeandwan.photos.domain.models.GridThumbnailSize

@Immutable
data class MediaGridState<T>(
    val gridItems: List<MediaGridItem<T>>,
    val thumbnailSize: GridThumbnailSize,
    val onSelectGridItem: (MediaGridItem<T>) -> Unit,
) {
    val size: Dp
        get() = getSize(thumbnailSize)
}

@Composable
fun <T> rememberMediaGridState(
    gridItems: List<MediaGridItem<T>> = emptyList(),
    thumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    onSelectGridItem: (MediaGridItem<T>) -> Unit = {},
): MediaGridState<T> =
    remember(gridItems, thumbnailSize, onSelectGridItem) {
        MediaGridState(
            gridItems,
            thumbnailSize,
            onSelectGridItem,
        )
    }
