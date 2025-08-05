package us.mikeandwan.photos.ui.controls.mediagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.domain.models.GridThumbnailSize

@Composable
fun <T> ImageGrid(state: ImageGridState<T>) {
    val size = remember(state.thumbnailSize) { getSize(state.thumbnailSize) }

    if(size > 0.dp) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = size),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(
                state.gridItems,
                key = { item -> item.id }
            ) {
                ImageGridImage(
                    item = it,
                    size = size,
                    onSelectImage = { item -> state.onSelectGridItem(item) }
                )
            }
        }
    }
}

fun getSize(size: GridThumbnailSize): Dp {
    return when (size) {
        GridThumbnailSize.ExtraSmall -> 60.dp
        GridThumbnailSize.Small -> 90.dp
        GridThumbnailSize.Medium -> 120.dp
        GridThumbnailSize.Large -> 180.dp
        GridThumbnailSize.Unspecified -> 0.dp
    }
}
