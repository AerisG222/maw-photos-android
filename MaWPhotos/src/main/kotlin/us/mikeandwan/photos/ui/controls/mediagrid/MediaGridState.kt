package us.mikeandwan.photos.ui.controls.mediagrid

import androidx.compose.runtime.Composable
import us.mikeandwan.photos.domain.models.GridThumbnailSize

data class MediaGridState<T> (
    val gridItems: List<MediaGridItem<T>>,
    val thumbnailSize: GridThumbnailSize,
    val onSelectGridItem: (MediaGridItem<T>) -> Unit
)

@Composable
fun <T> rememberMediaGridState(
    gridItems: List<MediaGridItem<T>> = emptyList(),
    thumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    onSelectGridItem: (MediaGridItem<T>) -> Unit = {}
): MediaGridState<T> {
    return MediaGridState(
        gridItems,
        thumbnailSize,
        onSelectGridItem
    )
}
