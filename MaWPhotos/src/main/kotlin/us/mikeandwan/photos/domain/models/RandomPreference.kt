package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RandomPreference(
    val slideshowIntervalSeconds: Int = 3,
    val showWidgetInfo: Boolean = true,
)
