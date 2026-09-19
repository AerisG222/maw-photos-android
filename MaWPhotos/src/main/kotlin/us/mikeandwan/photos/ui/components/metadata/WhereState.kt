package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.runtime.Composable
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.Place

/**
 * Where the media was taken, for the details sheet's Where card.
 *
 * [places] is null until the tab has been opened and the answer has come back, which is what lets
 * the card wait quietly rather than claiming the media was never placed while it is still asking.
 */
class WhereState(
    val places: List<Place>?,
    val fetchPlaces: () -> Unit,
    val onSelectPlace: (Uuid) -> Unit,
)

@Composable
fun rememberWhereState(
    places: List<Place>? = null,
    fetchPlaces: () -> Unit = {},
    onSelectPlace: (Uuid) -> Unit = {},
): WhereState =
    WhereState(
        places = places,
        fetchPlaces = fetchPlaces,
        onSelectPlace = onSelectPlace,
    )
