package us.mikeandwan.photos.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Category(
    val id: Uuid,
    val year: Int,
    val name: String,
    val effectiveDate: LocalDate,
    val modified: Instant,
    val isFavorite: Boolean,
    val teaser: List<MediaFile>
)
