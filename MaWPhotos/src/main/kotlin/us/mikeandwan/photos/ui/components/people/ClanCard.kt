package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.Person

private val FACE_SIZE = 36.dp

// a third of a face, which reads as a stack while still leaving each one recognisable
private val FACE_OVERLAP = 12.dp

// separates one face from the face it sits on, in the colour of the card behind them
private val FACE_RING = 1.5.dp

/**
 * One saved group of people.
 *
 * The card opens the clan's media, which is the common errand, so it gets the whole surface.
 * Managing the clan itself lives behind the menu beside the name.
 */
@Composable
fun ClanCard(
    clan: Clan,
    onSelect: (Clan) -> Unit,
    onEditMembers: (Clan) -> Unit,
    onRename: (Clan) -> Unit,
    onDelete: (Clan) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(220.dp)
            .clickable { onSelect(clan) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clan.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = pluralStringResource(
                            id = R.plurals.clan_member_count,
                            count = clan.members.size,
                            clan.members.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = stringResource(id = R.string.clan_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.clan_edit_people)) },
                            onClick = {
                                showMenu = false
                                onEditMembers(clan)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.clan_rename)) },
                            onClick = {
                                showMenu = false
                                onRename(clan)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.clan_delete)) },
                            onClick = {
                                showMenu = false
                                onDelete(clan)
                            },
                        )
                    }
                }
            }

            if (clan.members.isEmpty()) {
                // either nobody was ever added, or the members are no longer visible to this
                // caller - the API cannot tell those apart without saying who it dropped, so
                // neither can this
                Text(
                    text = stringResource(id = R.string.clan_no_people),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FaceStack(members = clan.members)
            }
        }
    }
}

/**
 * The clan's members, overlapped so however many there are, they occupy one row of the card.
 *
 * The row is measured rather than cut to a fixed count: as many faces as the card can hold are
 * drawn, and when they do not all fit the last slot becomes the count of the ones left out - so
 * every card ends at the same place regardless of how big the clan is.
 */
@Composable
private fun FaceStack(
    members: List<Person>,
    modifier: Modifier = Modifier,
) {
    val ringColor = CardDefaults.cardColors().containerColor

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // each face past the first only costs what it does not overlap
        val step = FACE_SIZE - FACE_OVERLAP
        val capacity = (((maxWidth - FACE_SIZE) / step).toInt() + 1).coerceAtLeast(1)
        val shown = if (members.size <= capacity) members.size else capacity - 1
        val hidden = members.size - shown

        Row(
            horizontalArrangement = Arrangement.spacedBy(-FACE_OVERLAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            members.take(shown).forEach { member ->
                PersonFace(
                    person = member,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(FACE_SIZE)
                        .border(FACE_RING, ringColor, CircleShape),
                )
            }

            if (hidden > 0) {
                HiddenFaceCount(count = hidden, ringColor = ringColor)
            }
        }
    }
}

/** The faces the row had no room for, counted in a slot the same size as one of them. */
@Composable
private fun HiddenFaceCount(
    count: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(id = R.string.clan_more_people_description, count)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(FACE_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(FACE_RING, ringColor, CircleShape)
            // the badge says "+3" on screen, which reads as nothing much out loud
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(id = R.string.clan_more_people, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClanCardPreview() {
    ClanCard(
        clan = Clan(
            id = Uuid.random(),
            name = "The Kids",
            members = (1..4).map { Person(Uuid.random(), "Person $it", null, it, false) },
        ),
        onSelect = {},
        onEditMembers = {},
        onRename = {},
        onDelete = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ClanCardOverflowPreview() {
    ClanCard(
        clan = Clan(
            id = Uuid.random(),
            name = "Everybody",
            members = (1..24).map { Person(Uuid.random(), "Person $it", null, it, false) },
        ),
        onSelect = {},
        onEditMembers = {},
        onRename = {},
        onDelete = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ClanCardEmptyPreview() {
    ClanCard(
        clan = Clan(id = Uuid.random(), name = "Nobody Yet", members = emptyList()),
        onSelect = {},
        onEditMembers = {},
        onRename = {},
        onDelete = {},
    )
}
