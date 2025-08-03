package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CommentPhoto(
    val photoId: Uuid,
    val comment: String
)
