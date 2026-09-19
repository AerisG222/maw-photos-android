package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.runtime.Composable
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.MediaFaces

/**
 * Who is in the photograph, for the details sheet's Who card.
 *
 * [MediaFaces] is carried whole rather than unpacked, because the people and the count of faces
 * nobody has named are two halves of the same answer - a list of three names means something
 * different when there is a fourth face the app cannot place.
 *
 * Null until the tab has been opened and the answer has come back, which is what lets the card wait
 * quietly rather than saying nobody is here while it is still asking.
 */
class WhoState(
    val faces: MediaFaces?,
    val fetchFaces: () -> Unit,
    val onSelectPerson: (Uuid) -> Unit,
)

@Composable
fun rememberWhoState(
    faces: MediaFaces? = null,
    fetchFaces: () -> Unit = {},
    onSelectPerson: (Uuid) -> Unit = {},
): WhoState =
    WhoState(
        faces = faces,
        fetchFaces = fetchFaces,
        onSelectPerson = onSelectPerson,
    )
