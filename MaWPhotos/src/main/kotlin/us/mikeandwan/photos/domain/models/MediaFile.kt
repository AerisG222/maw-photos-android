package us.mikeandwan.photos.domain.models

data class MediaFile(
    val scale: Scale,
    val type: MediaFileType,
    val path: String
)
