package us.mikeandwan.photos.domain.models

/**
 * How the places area draws what it lists.
 *
 * Only the category labels for now - the tiles in the tree take their size from the place itself,
 * and the media a place holds is drawn with the media preferences like every other feed.
 */
data class PlacePreference(
    val showCategoryYear: Boolean = true,
    val showCategoryTitle: Boolean = true,
)
