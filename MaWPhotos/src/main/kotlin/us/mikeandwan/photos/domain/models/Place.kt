package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

/**
 * The three levels of the place tree, as the API names them.
 *
 * A country's children are usually its states, but not always - Macao and Hong Kong have no state
 * level, so their cities hang straight off the country. That is why the API returns a kind per
 * place rather than leaving a client to infer one from how deep it drilled, and why nothing here
 * derives a level from depth.
 */
enum class PlaceKind(
    // what the API calls it
    val apiValue: String,
) {
    Country("country"),
    State("state"),
    City("city"),
    ;

    companion object {
        // anything unrecognised is treated as the bottom of the tree rather than dropped: a kind
        // this build has never heard of still names a real place with real media behind it
        fun fromApi(value: String): PlaceKind = entries.firstOrNull { it.apiValue == value } ?: City
    }
}

/**
 * A browsable location: a country, a state or a city.
 *
 * Deliberately flat, mirroring the API - the hierarchy is expressed by [parentId] alone, because a
 * client asks for one level at a time and never has a use for the whole tree at once.
 *
 * [mediaCount] covers the whole subtree and is scoped by the API to what the caller may see, so it
 * can be shown as-is.
 *
 * [coverUrl] is an admin's hand picked photograph representing the place. Most places have none,
 * which is why a tile draws an icon for its kind rather than assuming a photograph is there.
 *
 * The API also returns the names above a place, which is what makes a search result from anywhere
 * in the tree legible. Nothing here carries them: this client only ever lists one level at a time,
 * and the breadcrumb above the listing already says where that level sits.
 */
data class Place(
    val id: Uuid,
    val parentId: Uuid?,
    val kind: PlaceKind,
    val name: String,
    val mediaCount: Int,
    val coverUrl: String?,
    // how many places sit directly inside this one that the caller can see.  it has to come from
    // the API: a city having no children follows from its kind, but a state whose only cities sit
    // in categories this caller cannot reach is just as much a leaf to them.
    val childCount: Int,
) {
    /**
     * Whether drilling into this place would show anything.
     *
     * A place with nothing inside it is not a lesser place - at the bottom of the tree the
     * photographs are the answer, so this decides where a tile leads rather than whether it works.
     */
    val isLeaf: Boolean
        get() = childCount == 0
}

/**
 * One rung of the breadcrumb above a place.
 *
 * Not a [Place]: it labels a path rather than offering a tile, so it carries no counts and no
 * cover. The API returns the chain in order, country first, and includes the place itself.
 */
data class PlaceAncestor(
    val id: Uuid,
    val kind: PlaceKind,
    val name: String,
)
