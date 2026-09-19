package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceKind
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.places.PlaceCover
import us.mikeandwan.photos.ui.components.places.labelId

private val COVER_SIZE = 48.dp
private val COVER_SHAPE = RoundedCornerShape(4.dp)

/**
 * Where the media was taken, broadest first: the country, then the state or region, then the city.
 *
 * All three are listed rather than only the narrowest, because each of them is a way into a
 * different amount of the library - the city is where this was, the country is everywhere else
 * that trip went.
 */
@Composable
fun WhereScreen(
    whereState: WhereState,
    modifier: Modifier = Modifier,
) {
    val places = whereState.places

    if (places == null) {
        Loading(modifier = modifier)
        return
    }

    if (places.isEmpty()) {
        DetailMessage(
            text = stringResource(id = R.string.media_detail_where_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(modifier.then(Modifier.fillMaxSize())) {
        items(places, key = { it.id }) { place ->
            PlaceRow(
                place = place,
                onSelect = { whereState.onSelectPlace(place.id) },
            )
        }
    }
}

@Composable
private fun PlaceRow(
    place: Place,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PlaceCover(
            kind = place.kind,
            coverUrl = place.coverUrl,
            shape = COVER_SHAPE,
            modifier = Modifier.size(COVER_SIZE),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // the kind as well as the name, because "Washington" alone does not say whether this
            // row opens a state or the city inside a different one
            Text(
                text = stringResource(id = place.kind.labelId()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // everything at this place and beneath it, which is what the row opens
            Text(
                text = pluralStringResource(
                    id = R.plurals.media_detail_place_media_count,
                    count = place.mediaCount,
                    place.mediaCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WhereScreenPreview() {
    WhereScreen(
        whereState = WhereState(
            places = listOf(
                Place(Uuid.random(), null, PlaceKind.Country, "United States", 48213, null, 31),
                Place(Uuid.random(), Uuid.random(), PlaceKind.State, "Massachusetts", 2104, null, 9),
                Place(Uuid.random(), Uuid.random(), PlaceKind.City, "Boston", 921, null, 0),
            ),
            fetchPlaces = {},
            onSelectPlace = {},
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun WhereScreenEmptyPreview() {
    WhereScreen(
        whereState = WhereState(
            places = emptyList(),
            fetchPlaces = {},
            onSelectPlace = {},
        ),
    )
}
