package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

// someone the recognition pipeline has identified across the library.  mediaCount and isFavorite
// are both scoped by the API to the caller, so neither says anything about what anyone else can
// see.  the preferred face is whichever crop the pipeline picked to represent the cluster, and is
// absent until one has been published - so a person with no face at all is perfectly valid.
@Serializable
data class Person(
    val id: Uuid,
    val name: String,
    val slug: String? = null,
    val preferredFaceId: Uuid? = null,
    val preferredFaceUrl: String? = null,
    val mediaCount: Int,
    val isFavorite: Boolean,
)
