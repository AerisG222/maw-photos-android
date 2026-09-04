package us.mikeandwan.photos.ui.screens.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceAncestor
import us.mikeandwan.photos.domain.models.PlaceKind
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.places.PlaceCard
import us.mikeandwan.photos.ui.components.places.PlaceChain

// wide enough for a 4:3 cover with a name under it to stay legible, narrow enough that a phone
// still gets two of them across
private val TILE_MIN_WIDTH = 150.dp

/**
 * One level of the place tree: where you are, what is inside it, and the photographs of the whole
 * of it.
 *
 * Nothing here narrows the listing. The tree is three levels deep and each one is a screenful at
 * most, so on a phone it is walked rather than queried - the API's name search and kind filter are
 * left to the web client, where a wide screen and a keyboard make them worth the room.
 */
@Composable
fun PlacesScreen(
    uiState: PlacesUiState,
    // the places already read, for the covers on the breadcrumb - see PlaceChain
    knownPlaces: Map<Uuid, Place>,
    onSelectPlace: (Place) -> Unit,
    onSelectChainLink: (Uuid?) -> Unit,
    onViewMedia: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.placeId != null) {
            PlaceChain(
                chain = uiState.chain,
                covers = knownPlaces,
                onSelect = onSelectChainLink,
            )
        }

        // the photographs of the whole subtree, one tap away at every level.  a tile only offers
        // them at the bottom of the tree, so without this a country would have no way to its own.
        uiState.placeId?.let { placeId ->
            ViewMediaButton(
                mediaCount = uiState.place?.mediaCount,
                onClick = { onViewMedia(placeId) },
            )
        }

        when {
            uiState.isPlaceMissing && uiState.places.isEmpty() -> {
                Message(stringResource(id = R.string.places_missing))
            }

            uiState.isLoading -> {
                MediaGridSkeleton(thumbnailSize = GridThumbnailSize.Medium)
            }

            !uiState.showsChildren -> {
                // a leaf: the chain names it and the button above offers what is here, so a heading
                // saying there is nothing further down would only repeat what the screen already
                // shows
            }

            uiState.places.isEmpty() -> {
                Message(stringResource(id = R.string.places_none_found))
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = TILE_MIN_WIDTH),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.places, key = { it.id }) { place ->
                        PlaceCard(
                            place = place,
                            onSelect = onSelectPlace,
                            leadsToMedia = place.isLeaf,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewMediaButton(
    mediaCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_image),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )

        Text(
            // the count only turns up once the place itself has landed, and the button works
            // perfectly well before it does
            text = when (mediaCount) {
                null -> stringResource(id = R.string.places_view_media)
                else -> stringResource(id = R.string.places_view_media_count, mediaCount)
            },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}

private fun previewPlace(
    name: String,
    kind: PlaceKind,
    mediaCount: Int,
    childCount: Int,
) = Place(
    id = Uuid.random(),
    parentId = null,
    kind = kind,
    name = name,
    mediaCount = mediaCount,
    coverUrl = null,
    childCount = childCount,
)

@Preview(showBackground = true)
@Composable
private fun PlacesScreenRootPreview() {
    PlacesScreen(
        uiState = PlacesUiState(
            places = listOf(
                previewPlace("United States", PlaceKind.Country, 48213, 31),
                previewPlace("Canada", PlaceKind.Country, 1204, 4),
                previewPlace("Hong Kong", PlaceKind.Country, 318, 1),
            ),
            isLoading = false,
        ),
        knownPlaces = emptyMap(),
        onSelectPlace = {},
        onSelectChainLink = {},
        onViewMedia = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PlacesScreenDrilledInPreview() {
    val state = previewPlace("Massachusetts", PlaceKind.State, 4213, 12)

    PlacesScreen(
        uiState = PlacesUiState(
            placeId = state.id,
            place = state,
            chain = listOf(
                PlaceAncestor(Uuid.random(), PlaceKind.Country, "United States"),
                PlaceAncestor(state.id, PlaceKind.State, "Massachusetts"),
            ),
            places = listOf(
                previewPlace("Boston", PlaceKind.City, 921, 0),
                previewPlace("Cambridge", PlaceKind.City, 204, 0),
            ),
            isLoading = false,
        ),
        knownPlaces = mapOf(state.id to state),
        onSelectPlace = {},
        onSelectChainLink = {},
        onViewMedia = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PlacesScreenLeafPreview() {
    val city = previewPlace("Boston", PlaceKind.City, 921, 0)

    PlacesScreen(
        uiState = PlacesUiState(
            placeId = city.id,
            place = city,
            chain = listOf(
                PlaceAncestor(Uuid.random(), PlaceKind.Country, "United States"),
                PlaceAncestor(Uuid.random(), PlaceKind.State, "Massachusetts"),
                PlaceAncestor(city.id, PlaceKind.City, "Boston"),
            ),
            isLoading = false,
        ),
        knownPlaces = mapOf(city.id to city),
        onSelectPlace = {},
        onSelectChainLink = {},
        onViewMedia = {},
    )
}
