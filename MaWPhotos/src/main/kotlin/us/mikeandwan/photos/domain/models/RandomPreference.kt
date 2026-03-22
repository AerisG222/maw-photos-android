package us.mikeandwan.photos.domain.models

data class RandomPreference(
    val slideshowIntervalSeconds: Int = 3,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    val showMediaTypeIndicator: Boolean = true,
)
