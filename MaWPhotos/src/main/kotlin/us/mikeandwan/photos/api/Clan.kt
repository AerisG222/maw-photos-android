package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

// a caller's saved selection of people, so "the kids" can be picked once rather than reassembled
// from the face grid every time.  members arrive whole - face url, media count, favorite flag - so
// a clan can be drawn with its faces without a second call, and they are filtered by the same
// visibility rule the person list uses, which means a clan can hold fewer people than were saved
// into it, or none at all.
//
// the API also returns created and modified timestamps.  they are deliberately not declared:
// nothing here reads them, and an undeclared field is ignored, while a declared one that ever
// failed to parse would take the whole clan list down with it.
@Serializable
data class Clan(
    val id: Uuid,
    val name: String,
    val members: List<Person> = emptyList(),
)
