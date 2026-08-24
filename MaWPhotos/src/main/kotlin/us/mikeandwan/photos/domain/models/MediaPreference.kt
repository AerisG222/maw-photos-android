package us.mikeandwan.photos.domain.models

data class MediaPreference(
    val slideshowIntervalSeconds: Int = 3,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showMediaTypeIndicator: Boolean = true,
    val showFavoriteIndicator: Boolean = true,
    val showFaceHighlights: Boolean = false,
)
