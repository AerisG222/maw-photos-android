package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceKind
import us.mikeandwan.photos.ui.components.places.PlaceCover

private val THUMBNAIL_WIDTH = 40.dp
private val THUMBNAIL_HEIGHT = 30.dp

/**
 * The countries behind the rail's places entry.
 *
 * The root of the tree rather than wherever the user has drilled to: from any depth this is one tap
 * to another branch, and the breadcrumb on the screen already handles moving within the branch you
 * are in.
 *
 * Only what has already been loaded is listed - the places area is not reachable without the screen
 * that lists them.
 */
@Composable
fun PlaceListMenu(
    countries: List<Place>,
    onPlaceSelected: (Place) -> Unit,
    onAllPlacesSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        item(key = "allPlaces") {
            Text(
                text = stringResource(id = R.string.places_all),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAllPlacesSelected() }
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
            )

            MenuDivider()
        }

        if (countries.isEmpty()) {
            item(key = "countriesEmpty") {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.places_menu_none),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        } else {
            items(countries, key = { country -> "place-${country.id}" }) { country ->
                CountryListItem(
                    place = country,
                    onPlaceSelected = onPlaceSelected,
                )

                MenuDivider()
            }
        }
    }
}

@Composable
private fun CountryListItem(
    place: Place,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlaceSelected(place) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PlaceCover(
            kind = place.kind,
            coverUrl = place.coverUrl,
            modifier = Modifier
                .width(THUMBNAIL_WIDTH)
                .height(THUMBNAIL_HEIGHT),
        )

        Text(
            text = place.name,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceListMenuPreview() {
    PlaceListMenu(
        countries = listOf(
            Place(Uuid.random(), null, PlaceKind.Country, "United States", 48213, null, 31),
            Place(Uuid.random(), null, PlaceKind.Country, "Canada", 1204, null, 4),
        ),
        onPlaceSelected = {},
        onAllPlacesSelected = {},
    )
}
