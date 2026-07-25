package us.mikeandwan.photos.domain.models

data class RandomPreference(
    val slideshowIntervalSeconds: Int = 3,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showMediaTypeIndicator: Boolean = true,
    val showFavoriteIndicator: Boolean = true,
    val showWidgetInfo: Boolean = true,
)
