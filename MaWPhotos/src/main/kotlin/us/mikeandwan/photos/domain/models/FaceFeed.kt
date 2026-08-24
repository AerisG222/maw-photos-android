package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

/**
 * Whose media a face feed is showing.
 *
 * A person and a clan differ only in which endpoint answers; everything downstream - paging, the
 * filters, the grid, the pager - is identical, which is why they share one subject type rather than
 * one feed each.
 */
sealed class FaceFeedSubject {
    data class Person(
        val personId: Uuid,
    ) : FaceFeedSubject()

    data class Clan(
        val clanId: Uuid,
    ) : FaceFeedSubject()
}

/**
 * How a face feed is narrowed and ordered.
 *
 * [seed] is a seed rather than a "shuffle" flag because the feed is paged: the API orders by a hash
 * of the media id and this seed, so the same seed always yields the same order. Drawing a fresh one
 * per request would make the second page repeat and skip rows from the first. Null means the
 * default, newest first.
 */
data class FaceFeedFilter(
    val favoritesOnly: Boolean = false,
    val seed: Long? = null,
)
