package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExifScreen(
    exifState: ExifState
) {
    LazyColumn(Modifier.fillMaxSize()) {
//        itemsIndexed(
//            exifState.exifDisplay,
//            key = { index, item -> item.first }
//        ) { index, data ->
//            val bgColor = if (index % 2 == 0) {
//                MaterialTheme.colorScheme.surfaceVariant
//            } else {
//                MaterialTheme.colorScheme.surface
//            }
//
//            val txtColor = if (index % 2 == 0) {
//                MaterialTheme.colorScheme.onSurfaceVariant
//            } else {
//                MaterialTheme.colorScheme.onSurface
//            }
//
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .background(bgColor)
//            ) {
//                Text (
//                    text = data.first,
//                    color = txtColor,
//                    fontSize = 14.sp,
//                    modifier = Modifier
//                        .weight(1f)
//                        .padding(4.dp, 2.dp)
//                )
//                Text (
//                    text = data.second,
//                    color = txtColor,
//                    fontSize = 14.sp,
//                    modifier = Modifier
//                        .weight(1f)
//                        .padding(4.dp, 2.dp)
//                )
//            }
//        }
    }
}
