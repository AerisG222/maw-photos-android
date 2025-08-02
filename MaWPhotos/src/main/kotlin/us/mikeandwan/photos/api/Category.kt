package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import us.mikeandwan.photos.api.serializers.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Category constructor(
    val id: Uuid,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    val effectiveDate: Instant,
    @Serializable(with = InstantSerializer::class)
    val modified: Instant,
    val isFavorite: Boolean,
    val teaser: Media
)
