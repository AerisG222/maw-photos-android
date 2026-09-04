package us.mikeandwan.photos.ui.components.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.PlaceKind

/**
 * The photograph chosen to represent a place, at whatever size and shape the caller lays out.
 *
 * The icon for the place's kind sits underneath the image rather than being handed to Coil as a
 * fallback, so it shows through in both cases that leave nothing to draw: a place whose cover has
 * never been chosen - which is most of them, so the icon is the normal case rather than an error -
 * and a cover this device cannot decode, since covers are avif and that needs api 31.
 *
 * A country, a state and a city are worth telling apart at a glance while walking back up the
 * chain, which is why the icon follows the kind rather than being one shape for all three.
 *
 * [background] is what that icon sits on, and is asked for rather than fixed because the same frame
 * appears on three different grounds - a tile, a breadcrumb chip and a rail row. A caller passes its
 * own so the empty frame reads as part of the surface it is on rather than as a hole punched in it.
 */
@Composable
fun PlaceCover(
    kind: PlaceKind,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    // the icon is drawn faintly on whatever ground it was given.  the theme knows the right ink for
    // its own roles; anything else - a caller's own colour - falls back to the variant ink, which
    // is legible on every surface in this palette.
    val icon = contentColorFor(background).takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(background),
    ) {
        Icon(
            painter = painterResource(id = kind.iconId()),
            contentDescription = null,
            tint = icon.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxSize(0.5f),
        )

        AsyncImage(
            model = coverUrl,
            contentDescription = stringResource(id = R.string.place_cover_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun PlaceKind.iconId(): Int =
    when (this) {
        PlaceKind.Country -> R.drawable.ic_public
        PlaceKind.State -> R.drawable.ic_map
        PlaceKind.City -> R.drawable.ic_location_city
    }

// singular, for labelling one place rather than a listing of them
fun PlaceKind.labelId(): Int =
    when (this) {
        PlaceKind.Country -> R.string.place_kind_country
        PlaceKind.State -> R.string.place_kind_state
        PlaceKind.City -> R.string.place_kind_city
    }

@Preview(showBackground = true)
@Composable
private fun PlaceCoverPreview() {
    PlaceCover(
        kind = PlaceKind.Country,
        coverUrl = null,
        modifier = Modifier.size(120.dp, 90.dp),
    )
}
