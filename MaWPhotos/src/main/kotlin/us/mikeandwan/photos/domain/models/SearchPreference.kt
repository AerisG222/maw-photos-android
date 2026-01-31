package us.mikeandwan.photos.domain.models

data class SearchPreference(
    val id: Int,
    val recentQueryCountToSave: Int,
    val displayType: CategoryDisplayType,
    val gridThumbnailSize: GridThumbnailSize,
)
