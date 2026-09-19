package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchPreference(
    val recentQueryCountToSave: Int = 20,
    val displayType: CategoryDisplayType = CategoryDisplayType.Grid,
)
