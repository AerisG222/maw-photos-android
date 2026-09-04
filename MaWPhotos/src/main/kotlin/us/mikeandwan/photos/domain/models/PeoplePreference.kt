package us.mikeandwan.photos.domain.models

data class PeoplePreference(
    val sortBy: PersonSort = PersonSort.Name,
    val gridThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showNames: Boolean = true,
    val showMediaCounts: Boolean = true,
    val showClans: Boolean = true,
    val showCategoryYear: Boolean = true,
    val showCategoryTitle: Boolean = true,
)
