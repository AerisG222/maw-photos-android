package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

/**
 * How the places area draws what it lists.
 *
 * Only the category labels for now - the tiles in the tree take their size from the place itself,
 * and the media a place holds is drawn with the media preferences like every other feed.
 *
 * Kept apart from the people preferences: the two areas share a feed but are read differently, and
 * somebody who wants the year against every category while walking a country's photographs is not
 * thereby asking for it against a person's.
 */
@Serializable
data class PlacePreference(
    val showCategoryYear: Boolean = true,
    val showCategoryTitle: Boolean = true,
)
