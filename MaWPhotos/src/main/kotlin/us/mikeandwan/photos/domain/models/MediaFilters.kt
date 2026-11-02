package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

/**
 * What a listing of photos and videos can be narrowed to.
 *
 * Saved rather than held by the screen, so a listing comes back narrowed the way it was left, and
 * saved per area, because favorites means something different in each: a favorite person is not a
 * favorite photo. The Filters button's badge is what says a listing is narrowed, and the settings
 * screen shows the same choices.
 */
@Serializable
data class MediaFilters(
    val favoritesOnly: Boolean = false,
    // one type rather than two switches: both on would mean nothing
    val mediaType: MediaType? = null,
) {
    val activeCount: Int
        get() = (if (favoritesOnly) 1 else 0) + (if (mediaType != null) 1 else 0)

    val isActive: Boolean
        get() = activeCount > 0
}

/** How the API spells a media type when it is asked to narrow a listing to one. */
fun MediaType.toApiFilter(): String = name.lowercase()

fun List<Media>.filteredBy(filters: MediaFilters): List<Media> =
    if (filters.isActive) {
        filter { media ->
            (!filters.favoritesOnly || media.isFavorite) &&
                (filters.mediaType == null || media.type == filters.mediaType)
        }
    } else {
        this
    }

// favorites is the category's own flag rather than whether it holds a favorite photo, and the type
// is whether it holds any of that type
fun List<Category>.filteredCategoriesBy(filters: MediaFilters): List<Category> =
    if (filters.isActive) {
        filter { category ->
            (!filters.favoritesOnly || category.isFavorite) &&
                (filters.mediaType == null || filters.mediaType in category.mediaTypes)
        }
    } else {
        this
    }
