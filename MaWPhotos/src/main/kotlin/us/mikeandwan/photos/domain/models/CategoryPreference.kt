package us.mikeandwan.photos.domain.models

data class CategoryPreference(
    val displayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showMediaTypeIndicator: Boolean = true,
    val showFavoriteIndicator: Boolean = true,
)
