package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonElement

class ExifState(
    val exifDisplay: JsonElement?,
    val fetchExif: () -> Unit = {},
)

@Composable
fun rememberExifState(
    exif: JsonElement? = null,
    fetchExif: () -> Unit = {},
): ExifState {
    return ExifState(
        exifDisplay = exif,
        fetchExif = fetchExif
    )
}
