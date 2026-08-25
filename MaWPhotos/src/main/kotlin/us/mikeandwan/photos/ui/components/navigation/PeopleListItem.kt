package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.ui.components.people.PersonFace

private val AVATAR_SIZE = 32.dp

/**
 * One person in the rail, drawn with the same face the grid uses so the two read as the same list.
 */
@Composable
fun PersonListItem(
    person: Person,
    isActive: Boolean,
    onPersonSelected: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    MenuListItem(
        name = person.name,
        isActive = isActive,
        onClick = { onPersonSelected(person) },
        modifier = modifier,
    ) {
        PersonFace(
            person = person,
            shape = CircleShape,
            modifier = Modifier.size(AVATAR_SIZE),
        )
    }
}

/**
 * One clan in the rail.  A group has no face of its own, so it carries the same icon the rail's
 * people entry does rather than a sample of its members - at this size a row of crops would be
 * unreadable.
 */
@Composable
fun ClanListItem(
    name: String,
    isActive: Boolean,
    onClanSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MenuListItem(
        name = name,
        isActive = isActive,
        onClick = onClanSelected,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_people),
            contentDescription = null,
            tint = when (isActive) {
                true -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.size(AVATAR_SIZE),
        )
    }
}

@Composable
private fun MenuListItem(
    name: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val bgColor = when (isActive) {
        true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when (isActive) {
        true -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(color = bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        leading()

        Text(
            text = name,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PersonListItemPreview() {
    PersonListItem(
        person = Person(Uuid.random(), "Alice Anderson", null, 42, true),
        isActive = false,
        onPersonSelected = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PersonListItemActivePreview() {
    PersonListItem(
        person = Person(Uuid.random(), "Alice Anderson", null, 42, true),
        isActive = true,
        onPersonSelected = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ClanListItemPreview() {
    ClanListItem(
        name = "The Kids",
        isActive = false,
        onClanSelected = {},
    )
}
