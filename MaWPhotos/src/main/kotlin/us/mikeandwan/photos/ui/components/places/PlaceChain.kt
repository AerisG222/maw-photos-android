package us.mikeandwan.photos.ui.components.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceAncestor
import us.mikeandwan.photos.domain.models.PlaceKind

private val CHIP_SHAPE = RoundedCornerShape(4.dp)

private val THUMBNAIL_WIDTH = 40.dp
private val THUMBNAIL_HEIGHT = 30.dp

/**
 * Where in the tree the screen is, as the tree rather than as a sentence.
 *
 * Every rung is a jump, including the one you are on - it is where a place read again after a merge
 * or a move gets picked up, and it costs nothing to leave working.
 *
 * Chips with a thumbnail rather than the picture cards the tiles below use: covers are hand picked
 * and most places have none, so a strip of cards would be a row of empty frames. This way a rung
 * reads as its name with a picture when there is one, and gets richer on its own as covers are
 * chosen.
 *
 * [covers] is looked up per rung rather than carried on the ancestor, because the two halves come
 * from different reads - the chain from the ancestors endpoint, which always answers, and the cover
 * from each place, which arrives later or not at all.
 */
@Composable
fun PlaceChain(
    chain: List<PlaceAncestor>,
    covers: Map<Uuid, Place>,
    onSelect: (Uuid?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // three rungs and a root chip do not fit across a phone, and the rung you are on is the last
    // one - so the strip opens at its end rather than showing the country you started from and
    // hiding the place you are actually looking at.  the list clamps at the end of its content,
    // which leaves as much of the path in view as there is room for.
    LaunchedEffect(chain.lastOrNull()?.id) {
        if (chain.isNotEmpty()) {
            listState.scrollToItem(chain.size)
        }
    }

    LazyRow(
        state = listState,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        item(key = "root") {
            RootChip(onSelect = { onSelect(null) })
        }

        items(chain, key = { it.id }) { rung ->
            Separator()

            Rung(
                ancestor = rung,
                place = covers[rung.id],
                isCurrent = rung.id == chain.lastOrNull()?.id,
                onSelect = { onSelect(rung.id) },
            )
        }
    }
}

@Composable
private fun RootChip(
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(CHIP_SHAPE)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CHIP_SHAPE)
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_place),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )

        Text(
            text = stringResource(id = R.string.places_all),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun Separator(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.size(16.dp),
    )
}

@Composable
private fun Rung(
    ancestor: PlaceAncestor,
    place: Place?,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(CHIP_SHAPE)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(1.dp, borderColor, CHIP_SHAPE)
            .clickable { onSelect() }
            .padding(end = 8.dp),
    ) {
        PlaceCover(
            kind = ancestor.kind,
            coverUrl = place?.coverUrl,
            modifier = Modifier
                .width(THUMBNAIL_WIDTH)
                .height(THUMBNAIL_HEIGHT),
        )

        Text(
            text = ancestor.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // only on the rung you are on: the others are a path, not a subject
        if (isCurrent && place != null) {
            Text(
                text = place.mediaCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceChainPreview() {
    val countryId = Uuid.random()
    val stateId = Uuid.random()

    PlaceChain(
        chain = listOf(
            PlaceAncestor(countryId, PlaceKind.Country, "United States"),
            PlaceAncestor(stateId, PlaceKind.State, "Massachusetts"),
        ),
        covers = mapOf(
            stateId to Place(
                id = stateId,
                parentId = countryId,
                kind = PlaceKind.State,
                name = "Massachusetts",
                mediaCount = 4213,
                coverUrl = null,
                childCount = 12,
            ),
        ),
        onSelect = {},
    )
}
