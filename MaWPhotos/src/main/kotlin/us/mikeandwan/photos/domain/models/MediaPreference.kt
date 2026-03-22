package us.mikeandwan.photos.domain.models

data class MediaPreference(
    val slideshowIntervalSeconds: Int = 3,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    val showMediaTypeIndicator: Boolean = true,
)
