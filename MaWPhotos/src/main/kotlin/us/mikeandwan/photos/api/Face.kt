package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

// one detected face in a media item, for drawing an overlay.
//
// the box is normalised 0..1 against the full frame, so it applies at whatever scale happens to be
// on screen.  a detector may report slightly outside that range for a face the frame cuts off, so
// values are not assumed to be within it.
//
// personId is null both for a face nobody has been assigned to and for one whose person the caller
// is not allowed to know about - the API makes the two indistinguishable on purpose.
@Serializable
data class Face(
    val id: Uuid,
    val personId: Uuid? = null,
    val boxX: Float,
    val boxY: Float,
    val boxWidth: Float,
    val boxHeight: Float,
)
