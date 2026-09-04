package us.mikeandwan.photos.ui.components.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceKind

// covers are published at 320x240, so the tile is shaped to match
private const val COVER_ASPECT_RATIO = 4f / 3f

private val BADGE_SHAPE = RoundedCornerShape(4.dp)

// Stable selector for UI automation (baseline profile generation), like the person card's.
// Surfaced to UiAutomator via `testTagsAsResourceId` enabled at the app root. Keep in sync with the
// matching literal in the :baselineprofile module's BaselineProfileGenerator.
const val PLACE_CARD_TAG = "placeCard"

/**
 * One place in the browse.
 *
 * The whole tile is the drill-in, so the badges on it only say what is there rather than doing
 * anything: the kind, how many of the caller's media were taken here and everywhere beneath, and -
 * where the tile opens the photographs instead of the places inside - that it does. That last one
 * is shown rather than left to be discovered by tapping, so where a tile leads is legible before it
 * is followed.
 */
@Composable
fun PlaceCard(
    place: Place,
    onSelect: (Place) -> Unit,
    modifier: Modifier = Modifier,
    // whether the tile opens the photographs rather than the places inside
    leadsToMedia: Boolean = false,
) {
    // the cover frame is painted in the card's own colour, so a place with no cover - which is most
    // of them - reads as a card with a picture missing rather than as a card with a panel in it
    val colors = CardDefaults.cardColors()

    Card(
        colors = colors,
        modifier = modifier
            .fillMaxWidth()
            .testTag(PLACE_CARD_TAG)
            .clickable { onSelect(place) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_ASPECT_RATIO),
        ) {
            PlaceCover(
                kind = place.kind,
                coverUrl = place.coverUrl,
                background = colors.containerColor,
                modifier = Modifier.matchParentSize(),
            )

            Badge(
                text = stringResource(id = place.kind.labelId()),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            )

            Badge(
                text = place.mediaCount.toString(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )

            if (leadsToMedia) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(BADGE_SHAPE)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        )
                        .padding(3.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = stringResource(id = R.string.places_leads_to_media),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Text(
            text = place.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Badge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .clip(BADGE_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceCardPreview() {
    PlaceCard(
        place = Place(
            id = Uuid.random(),
            parentId = null,
            kind = PlaceKind.Country,
            name = "United States",
            mediaCount = 48213,
            coverUrl = null,
            childCount = 31,
        ),
        onSelect = {},
        modifier = Modifier.width(180.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceCardLeafPreview() {
    PlaceCard(
        place = Place(
            id = Uuid.random(),
            parentId = Uuid.random(),
            kind = PlaceKind.City,
            name = "Boston",
            mediaCount = 921,
            coverUrl = null,
            childCount = 0,
        ),
        onSelect = {},
        leadsToMedia = true,
        modifier = Modifier.width(180.dp),
    )
}
