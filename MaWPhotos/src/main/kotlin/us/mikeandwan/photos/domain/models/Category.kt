package us.mikeandwan.photos.domain.models

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class Category(
    val id: Uuid,
    val year: Int,
    val name: String,
    val effectiveDate: LocalDate,
    val modified: Instant,
    val isFavorite: Boolean,
    val teaser: List<MediaFile>,
    val mediaTypes: List<MediaType>,
)
