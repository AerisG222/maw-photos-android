package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.jsonObject

@Composable
fun ExifScreen(
    exifState: ExifState
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if( exifState.exifDisplay == null) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = "EXIF data unavailable",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            return@LazyColumn
        }

        for(exif in exifState.exifDisplay.jsonObject) {
            item {
                ExifHeader(exif.key)
            }

            var idx = 1

            for(detail in exif.value.jsonObject) {
                item {
                    ExifDetail(
                        if(idx % 2 == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        if(idx % 2 == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        detail.value.jsonObject
                    )

                    idx++
                }
            }
        }
    }
}
