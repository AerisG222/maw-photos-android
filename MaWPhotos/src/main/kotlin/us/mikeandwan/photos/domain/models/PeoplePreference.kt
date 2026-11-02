package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PeoplePreference(
    val sortBy: PersonSort = PersonSort.Name,
    val showNames: Boolean = true,
    val showMediaCounts: Boolean = true,
    // the clan row costs about a quarter of a phone screen before a single face is drawn, so it can
    // be folded away by whoever is here to look at people rather than at their groups
    val showClans: Boolean = true,
    // what a category says about itself when a person's or clan's categories are being listed.  a
    // face feed spans years, so both start on - see CategoryLabels
    val showCategoryYear: Boolean = true,
    val showCategoryTitle: Boolean = true,
    // two rather than one: the first narrows the list of people, the second the media of whoever is
    // opened from it, and wanting only favorite people does not mean only their favorite photos
    val filters: MediaFilters = MediaFilters(),
    val feedFilters: MediaFilters = MediaFilters(),
    val feedShuffled: Boolean = false,
)
