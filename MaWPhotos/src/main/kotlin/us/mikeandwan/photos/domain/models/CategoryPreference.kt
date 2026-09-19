package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CategoryPreference(
    val displayType: CategoryDisplayType = CategoryDisplayType.Grid,
)
