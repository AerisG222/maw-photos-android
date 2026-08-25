package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.Person

/**
 * The caller's clans, above the face grid they are assembled from.
 *
 * Laid out in a row rather than a grid: there are only ever a handful, and keeping them to one line
 * leaves the screen to the people, which is what most visits are for.
 */
@Composable
fun ClanSection(
    clans: List<Clan>,
    onSelect: (Clan) -> Unit,
    onEditMembers: (Clan) -> Unit,
    onRename: (Clan) -> Unit,
    onDelete: (Clan) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(clans, key = { clan -> clan.id }) { clan ->
            ClanCard(
                clan = clan,
                onSelect = onSelect,
                onEditMembers = onEditMembers,
                onRename = onRename,
                onDelete = onDelete,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClanSectionPreview() {
    ClanSection(
        clans = listOf(
            Clan(
                id = Uuid.random(),
                name = "The Kids",
                members = (1..3).map { Person(Uuid.random(), "Person $it", null, it, false) },
            ),
        ),
        onSelect = {},
        onEditMembers = {},
        onRename = {},
        onDelete = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ClanSectionEmptyPreview() {
    ClanSection(
        clans = emptyList(),
        onSelect = {},
        onEditMembers = {},
        onRename = {},
        onDelete = {},
    )
}
