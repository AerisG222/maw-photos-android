package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

/**
 * The strip the clan row folds into.
 *
 * Creating a clan lives here rather than inside the row, so it stays reachable when the row is put
 * away - which is the state somebody who mostly browses people will leave it in.
 */
@Composable
fun ClanSectionHeader(
    clanCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
            .padding(start = 12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.clan_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // the count is what makes a folded row worth unfolding, so it is on the header rather than
        // only inside
        Text(
            text = stringResource(id = R.string.clan_section_count, clanCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )

        Icon(
            painter = painterResource(
                id = if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            ),
            contentDescription = stringResource(
                id = if (expanded) R.string.clan_section_collapse else R.string.clan_section_expand,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onCreate) {
            Icon(
                painter = painterResource(id = R.drawable.ic_group_add),
                contentDescription = stringResource(id = R.string.clan_new),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClanSectionHeaderExpandedPreview() {
    ClanSectionHeader(
        clanCount = 3,
        expanded = true,
        onToggleExpanded = {},
        onCreate = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ClanSectionHeaderCollapsedPreview() {
    ClanSectionHeader(
        clanCount = 3,
        expanded = false,
        onToggleExpanded = {},
        onCreate = {},
    )
}
