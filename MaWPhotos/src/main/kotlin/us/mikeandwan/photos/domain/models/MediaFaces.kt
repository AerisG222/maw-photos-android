package us.mikeandwan.photos.domain.models

/**
 * Everything the recognition pipeline says about one media item: the boxes to draw over it, the
 * people those boxes resolved to, and how many faces were left over.
 *
 * One value rather than three flows because the overlay and the details sheet's Who card are two
 * views of the same answer. A card naming three people over an overlay drawing four boxes would be
 * worse than either of them alone, and keeping the resolution in one place is what makes that
 * impossible rather than merely unlikely.
 */
data class MediaFaces(
    val highlights: List<FaceHighlight> = emptyList(),
    // each person once, however many of their faces were detected, ordered by name - the order the
    // boxes were detected in means nothing to somebody reading a list of names
    val people: List<Person> = emptyList(),
    // faces with nobody to name: unassigned, or a person this caller may not be allowed to know
    // about - the API makes those two indistinguishable on purpose.  counted rather than dropped,
    // because a list that left them out would look complete when it is not.
    val unnamedCount: Int = 0,
)
