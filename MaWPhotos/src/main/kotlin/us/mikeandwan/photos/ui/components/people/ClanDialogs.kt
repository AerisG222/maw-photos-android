package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.ClanRules

/**
 * Names a clan, on the way to creating one or when renaming an existing one.
 *
 * The name is asked for last when creating: it is easier to name a group once you can see who is in
 * it, which is why the picker submits into this rather than starting from it.
 */
@Composable
fun ClanNameDialog(
    title: String,
    submitLabel: String,
    initialName: String,
    memberCount: Int?,
    isSaving: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    // keyed on the initial name so reopening the dialog for another clan starts from that clan's
    // name rather than whatever was last typed
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }

    val trimmed = name.trim()
    val isTooLong = trimmed.length > ClanRules.MAX_NAME_LENGTH
    val canSubmit = trimmed.isNotEmpty() && !isTooLong && !isSaving

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = title) },
        text = {
            Column {
                if (memberCount != null) {
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.clan_member_count,
                            count = memberCount,
                            memberCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = isTooLong || error != null,
                    label = { Text(text = stringResource(id = R.string.clan_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                val message = when {
                    isTooLong -> stringResource(
                        id = R.string.clan_name_too_long,
                        ClanRules.MAX_NAME_LENGTH,
                    )

                    else -> error
                }

                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(trimmed) },
                enabled = canSubmit,
            ) {
                Text(text = submitLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun ClanDeleteDialog(
    clan: Clan,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(id = R.string.clan_delete_title, clan.name)) },
        // worth saying outright: deleting a clan is about the shortcut, not about the people in it
        text = { Text(text = stringResource(id = R.string.clan_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text(text = stringResource(id = R.string.clan_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun ClanNameDialogCreatePreview() {
    ClanNameDialog(
        title = "Name Your Clan",
        submitLabel = "Create",
        initialName = "",
        memberCount = 4,
        isSaving = false,
        error = null,
        onSubmit = {},
        onCancel = {},
    )
}

@Preview
@Composable
private fun ClanNameDialogErrorPreview() {
    ClanNameDialog(
        title = "Rename Clan",
        submitLabel = "Save",
        initialName = "The Kids",
        memberCount = null,
        isSaving = false,
        error = "You already have a clan with that name.",
        onSubmit = {},
        onCancel = {},
    )
}

@Preview
@Composable
private fun ClanDeleteDialogPreview() {
    ClanDeleteDialog(
        clan = Clan(id = kotlin.uuid.Uuid.random(), name = "The Kids", members = emptyList()),
        isSaving = false,
        onConfirm = {},
        onCancel = {},
    )
}
