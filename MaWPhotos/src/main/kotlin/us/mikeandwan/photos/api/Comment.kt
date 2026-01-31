package us.mikeandwan.photos.api

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.api.serializers.InstantSerializer

@Serializable
data class Comment(
    val commentId: Uuid,
    @Serializable(with = InstantSerializer::class)
    val created: Instant,
    val createdBy: String,
    @Serializable(with = InstantSerializer::class)
    val modified: Instant,
    val body: String,
)
