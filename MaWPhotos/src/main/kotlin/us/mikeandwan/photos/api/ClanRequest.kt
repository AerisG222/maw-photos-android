package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

// personIds is nullable on an update because an omitted list means "leave the membership alone"
// rather than "empty it", which is what lets a rename avoid resending everyone
@Serializable
data class ClanRequest(
    val name: String,
    val personIds: List<Uuid>? = null,
)

// the whole membership rather than a delta: the picker already knows the full set, which makes the
// call idempotent and means a lost response cannot leave a clan half updated
@Serializable
data class ClanPersonsRequest(
    val personIds: List<Uuid>,
)
