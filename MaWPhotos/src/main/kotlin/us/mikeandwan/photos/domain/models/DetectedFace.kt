package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

// box coordinates are normalised 0..1 against the source frame, and are not guaranteed to be inside
// that range - a face the frame cuts off is reported slightly outside it, so clamp when drawing
data class DetectedFace(
    val id: Uuid,
    // null while a face is unassigned, and also when its person is one this caller may not know
    // about.  the two are indistinguishable by design.
    val personId: Uuid?,
    val boxX: Float,
    val boxY: Float,
    val boxWidth: Float,
    val boxHeight: Float,
)
