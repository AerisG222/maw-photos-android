package us.mikeandwan.photos.api

import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.api.serializers.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Category(
    val id: Uuid,
    val name: String,
    @Serializable(with = LocalDateIso8601Serializer::class)
    val effectiveDate: LocalDate,
    @Serializable(with = InstantSerializer::class)
    val modified: Instant,
    val isFavorite: Boolean,
    val teaser: Media
)
