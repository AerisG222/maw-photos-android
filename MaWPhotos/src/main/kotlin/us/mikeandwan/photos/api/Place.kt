package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

// a browsable location - a country, a state or a city.
//
// deliberately flat, mirroring the API: the hierarchy is expressed by parentId alone, because a
// client asks for one level at a time and never has a use for the whole tree at once.
//
// kind is returned rather than inferred from how deep the caller drilled.  a country's children are
// usually its states, but not always - Macao and Hong Kong have no state level, so their cities
// hang straight off the country and a single listing can mix the two.
//
// mediaCount is scoped by the API to the caller and covers the whole subtree, so a country's count
// includes everything in its states and their cities.
//
// coverUrl is an admin's hand picked photograph representing the place, absolute and served from a
// directory that skips the per-file access check - so it renders for anyone signed in.  it carries
// a ?v= stamped from when the cover was published, which is what makes a replacement visible
// immediately rather than after a cache expires.  most places have none.
@Serializable
data class Place(
    val id: Uuid,
    val parentId: Uuid? = null,
    val kind: String,
    val name: String,
    val slug: String? = null,
    val mediaCount: Int,
    // the names above this place, root first and excluding itself.  empty for a country.  it is
    // what makes a search result legible: the library holds two cities called Zhuhai, both under a
    // parent called Guangdong, and only the grandparent tells them apart.
    val ancestorNames: List<String> = emptyList(),
    val coverUrl: String? = null,
    val coverMediaId: Uuid? = null,
    // how many places sit directly inside this one that the caller can see.  it has to come from
    // the API: a city having no children follows from its kind, but a state whose only cities sit
    // in categories this caller cannot reach is just as much a leaf to them.
    val childCount: Int,
)

// one rung of the breadcrumb above a place.  deliberately not a Place - it labels a path rather
// than offering a tile, so it carries no counts and no cover.  depth is 1-based from the country,
// and the API returns the chain in order, including the place itself.
@Serializable
data class PlaceAncestor(
    val id: Uuid,
    val parentId: Uuid? = null,
    val kind: String,
    val name: String,
    val slug: String? = null,
    val depth: Int,
)
