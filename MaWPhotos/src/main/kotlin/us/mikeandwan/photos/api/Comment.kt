package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import us.mikeandwan.photos.api.serializers.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Comment(
    val commentId: Uuid,
    @Serializable(with = InstantSerializer::class)
    val created: Instant,
    val createdBy: String,
    @Serializable(with = InstantSerializer::class)
    val modified: Instant,
    val body: String
)
