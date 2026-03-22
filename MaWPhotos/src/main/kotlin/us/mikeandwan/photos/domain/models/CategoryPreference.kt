package us.mikeandwan.photos.domain.models

data class CategoryPreference(
    val displayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Unspecified,
    val showMediaTypeIndicator: Boolean = true,
)
