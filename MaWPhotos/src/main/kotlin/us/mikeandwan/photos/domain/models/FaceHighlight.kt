package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

/**
 * A detected face together with the name to draw beside it, where there is one.
 *
 * The box comes from the recognition pipeline and the name from the people list, which are two
 * separate things the app holds - so a face can perfectly well be drawn before its name is known,
 * and picks the name up when the list is read.
 */
data class FaceHighlight(
    val id: Uuid,
    // null both for a face nobody has been assigned to and for one whose person the caller is not
    // allowed to know about - the API makes those two indistinguishable on purpose
    val personId: Uuid?,
    // null when there is nobody to name, and also when there is but the people list has not been
    // read yet, or no longer holds them.  a box without a label is a normal thing to draw.
    val name: String?,
    val boxX: Float,
    val boxY: Float,
    val boxWidth: Float,
    val boxHeight: Float,
)
