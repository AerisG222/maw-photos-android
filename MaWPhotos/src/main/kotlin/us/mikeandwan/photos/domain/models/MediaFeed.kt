package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

/**
 * What a media feed is showing: the media one person appears in, the media anyone in a clan appears
 * in, or the media taken at a place.
 *
 * The three differ only in which endpoint answers; everything downstream - paging, the filters, the
 * shuffle, the grid, the pager - is identical, which is why they share one subject type rather than
 * one feed each.
 */
sealed class MediaFeedSubject {
    data class Person(
        val personId: Uuid,
    ) : MediaFeedSubject()

    data class Clan(
        val clanId: Uuid,
    ) : MediaFeedSubject()

    /**
     * A country, a state or a city - and everything beneath it, so a country's feed holds the
     * photographs from every one of its states and cities.
     */
    data class Place(
        val placeId: Uuid,
    ) : MediaFeedSubject()

    /**
     * Which area of the app a feed over this subject belongs to.
     *
     * A feed is a way into the library from somewhere, not an area of its own, so the entry the
     * user came in through stays lit in the rail while they are inside one.
     */
    val navigationArea: NavigationArea
        get() = when (this) {
            is Person, is Clan -> NavigationArea.People
            is Place -> NavigationArea.Place
        }
}

/**
 * How a media feed is narrowed and ordered.
 *
 * [seed] is a seed rather than a "shuffle" flag because the feed is paged: the API orders by a hash
 * of the media id and this seed, so the same seed always yields the same order. Drawing a fresh one
 * per request would make the second page repeat and skip rows from the first. Null means the
 * default, newest first.
 */
data class MediaFeedFilter(
    val favoritesOnly: Boolean = false,
    val seed: Long? = null,
)

/**
 * What a category says about itself in a feed's category listing, beyond its teaser.
 *
 * Both start on. The list view has always drawn them, so a toggle only ever takes something away
 * there; the grid is where they are new, and a wall of unlabelled teasers is the harder of the two
 * to read when the categories span years.
 *
 * Saved rather than held for the session, and saved per area: the people preference answers for a
 * person or a clan, the places one for a place. The two areas share this feed but are read
 * differently, and wanting the year against every category while walking a country's photographs is
 * not the same as wanting it against one person's.
 */
data class CategoryLabels(
    val showYear: Boolean = true,
    val showTitle: Boolean = true,
)
