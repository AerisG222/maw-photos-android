package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.Person

/**
 * The clans and people behind the rail's people entry.
 *
 * Both lists live in the one panel, in the order the grid presents them, so moving from one
 * person's media to another's - or to a clan's - is a single tap rather than a trip back through
 * the grid.  Only what has already been loaded is listed; the people area is not reachable without
 * the screen that loads it.
 */
@Composable
fun PeopleListMenu(
    clans: List<Clan>,
    people: List<Person>,
    activeSubject: MediaFeedSubject?,
    onClanSelected: (Clan) -> Unit,
    onPersonSelected: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeClanId = (activeSubject as? MediaFeedSubject.Clan)?.clanId
    val activePersonId = (activeSubject as? MediaFeedSubject.Person)?.personId

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (clans.isNotEmpty()) {
            item(key = "clanHeader") {
                SectionHeader(text = stringResource(id = R.string.clan_section_title))
            }

            items(clans, key = { clan -> "clan-${clan.id}" }) { clan ->
                ClanListItem(
                    name = clan.name,
                    isActive = clan.id == activeClanId,
                    onClanSelected = { onClanSelected(clan) },
                )

                MenuDivider()
            }
        }

        item(key = "peopleHeader") {
            SectionHeader(text = stringResource(id = R.string.people_section_title))
        }

        if (people.isEmpty()) {
            item(key = "peopleEmpty") {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.people_menu_none),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        } else {
            items(people, key = { person -> "person-${person.id}" }) { person ->
                PersonListItem(
                    person = person,
                    isActive = person.id == activePersonId,
                    onPersonSelected = onPersonSelected,
                )

                MenuDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
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
private fun PeopleListMenuPreview() {
    val people = listOf(
        Person(Uuid.random(), "Alice Anderson", null, 42, true),
        Person(Uuid.random(), "Bob Brown", null, 17, false),
        Person(Uuid.random(), "Carol Clark", null, 3, false),
    )

    PeopleListMenu(
        clans = listOf(Clan(Uuid.random(), "The Kids", people)),
        people = people,
        activeSubject = MediaFeedSubject.Person(people[1].id),
        onClanSelected = {},
        onPersonSelected = {},
    )
}
