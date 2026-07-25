package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.ui.components.loading.rememberShimmerBrush

// enough to cover the tallest screen at the largest thumbnail size - the grid only composes what
// is actually visible
private const val PLACEHOLDER_COUNT = 30

/**
 * Stands in for [MediaGrid] while its content loads, laid out at the size the real thumbnails will
 * use so the content does not jump when it arrives.
 */
@Composable
fun MediaGridSkeleton(
    thumbnailSize: GridThumbnailSize,
    modifier: Modifier = Modifier,
) {
    val size = getSize(thumbnailSize)
    val brush = rememberShimmerBrush()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = size),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        items(PLACEHOLDER_COUNT) {
            Box(
                modifier = Modifier
                    .height(size)
                    .fillMaxWidth()
                    .background(brush),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaGridSkeletonPreview() {
    MediaGridSkeleton(thumbnailSize = GridThumbnailSize.Medium)
}
