package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.ClanRules

/**
 * Shown in place of the clan row while people are being picked, so the count and the way out stay in
 * reach however far down the face grid the user has scrolled.
 */
@Composable
fun SelectionBar(
    title: String,
    selectedCount: Int,
    submitLabel: String,
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // checked here as well as by the API, so a picker that has gone too far says so before the
    // request rather than after it
    val overLimit = selectedCount > ClanRules.MAX_MEMBERS

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = pluralStringResource(
                        id = R.plurals.clan_selected_count,
                        count = selectedCount,
                        selectedCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(
                onClick = onClear,
                enabled = selectedCount > 0 && !isSaving,
            ) {
                Text(text = stringResource(id = R.string.clan_clear))
            }

            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text(text = stringResource(id = R.string.cancel))
            }

            Button(
                onClick = onSubmit,
                enabled = canSubmit && !isSaving && !overLimit,
            ) {
                Text(text = submitLabel)
            }
        }

        if (overLimit) {
            Text(
                text = stringResource(id = R.string.clan_too_many_people, ClanRules.MAX_MEMBERS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionBarPreview() {
    SelectionBar(
        title = "New clan",
        selectedCount = 3,
        submitLabel = "Name Clan",
        canSubmit = true,
        isSaving = false,
        onSubmit = {},
        onClear = {},
        onCancel = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SelectionBarOverLimitPreview() {
    SelectionBar(
        title = "Members of The Kids",
        selectedCount = ClanRules.MAX_MEMBERS + 1,
        submitLabel = "Save People",
        canSubmit = true,
        isSaving = false,
        onSubmit = {},
        onClear = {},
        onCancel = {},
    )
}
