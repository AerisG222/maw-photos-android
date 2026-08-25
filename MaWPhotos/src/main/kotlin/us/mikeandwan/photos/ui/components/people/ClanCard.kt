package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.Person

// enough faces to recognise the clan at a glance; the rest are counted
private const val FACES_SHOWN = 5

private val FACE_SIZE = 36.dp

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
    val hidden = (clan.members.size - FACES_SHOWN).coerceAtLeast(0)

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    clan.members.take(FACES_SHOWN).forEach { member ->
                        PersonFace(
                            person = member,
                            shape = CircleShape,
                            modifier = Modifier.size(FACE_SIZE),
                        )
                    }

                    if (hidden > 0) {
                        Text(
                            text = stringResource(id = R.string.clan_more_people, hidden),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClanCardPreview() {
    ClanCard(
        clan = Clan(
            id = Uuid.random(),
            name = "The Kids",
            members = (1..7).map { Person(Uuid.random(), "Person $it", null, it, false) },
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
