package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val body: String
)
