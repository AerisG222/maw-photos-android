package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable

@Serializable
data class AccountStatus(
    val status: String,
    val isAdmin: Boolean
)
