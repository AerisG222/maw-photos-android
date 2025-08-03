package us.mikeandwan.photos.domain.models

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Category(
    val id: Uuid,
    val year: Int,
    val name: String,
    val effectiveDate: Instant,
    val modified: Instant,
    val isFavorite: Boolean,
    val teaser: List<MediaFile>
)
