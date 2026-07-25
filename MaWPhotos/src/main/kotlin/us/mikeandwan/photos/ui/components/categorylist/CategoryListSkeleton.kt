package us.mikeandwan.photos.ui.components.categorylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.ui.components.loading.rememberShimmerBrush

private const val PLACEHOLDER_COUNT = 12
private const val NAME_WIDTH_FRACTION = 0.6f

/**
 * Stands in for [CategoryList] while its content loads, mirroring the thumbnail and name layout of
 * a real row.
 */
@Composable
fun CategoryListSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    LazyColumn(
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        items(PLACEHOLDER_COUNT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(brush),
                )

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(NAME_WIDTH_FRACTION)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryListSkeletonPreview() {
    CategoryListSkeleton()
}
