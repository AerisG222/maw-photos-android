package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreference(
    val doNotify: Boolean = false,
    val doVibrate: Boolean = true,
)
