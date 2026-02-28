package us.mikeandwan.photos.ui.components.metadata

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
): ExifState =
    ExifState(
        exifDisplay = exif,
        fetchExif = fetchExif,
    )
