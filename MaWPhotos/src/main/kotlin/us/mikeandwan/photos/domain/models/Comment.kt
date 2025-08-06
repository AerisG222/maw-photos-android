package us.mikeandwan.photos.domain.models

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Comment(
    val commentId: Uuid,
    val created: Instant,
    val createdBy: String,
    val modified: Instant,
    val body: String
)
