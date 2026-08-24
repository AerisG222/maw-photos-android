package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

// the API also returns a slug and the id of the preferred face.  neither is carried here: the slug
// addresses a person in web urls, and the face id only ever appears inside the url the API has
// already composed.
data class Person(
    val id: Uuid,
    val name: String,
    // null until the pipeline has published a crop for them, which is a normal state rather than a
    // loading one - see PersonCard, which draws a silhouette instead
    val preferredFaceUrl: String?,
    val mediaCount: Int,
    val isFavorite: Boolean,
)
