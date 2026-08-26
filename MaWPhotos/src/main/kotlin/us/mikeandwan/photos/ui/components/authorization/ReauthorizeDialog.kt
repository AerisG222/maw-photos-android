package us.mikeandwan.photos.ui.components.authorization

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.R

/**
 * Asks a signed in user to sign in again so their credentials pick up face recognition.
 *
 * This is offered on launch rather than left in Settings alone: the scope cannot be widened without
 * a fresh login, and a user whose sign in predates the feature otherwise just sees the people area
 * missing with nothing to say why.  Dismissing holds for the rest of the session - see
 * MawPhotosAppViewModel.showReauthorizePrompt - so it asks once and the next launch asks again.
 */
@Composable
fun ReauthorizeDialog(
    onReauthorize: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.reauthorize_face_recognition_title)) },
        text = {
            Text(text = stringResource(id = R.string.reauthorize_face_recognition_message))
        },
        confirmButton = {
            TextButton(onClick = onReauthorize) {
                Text(text = stringResource(id = R.string.settings_face_recognition_reauthorize))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.reauthorize_not_now))
            }
        },
    )
}

@Preview
@Composable
fun ReauthorizeDialogPreview() {
    ReauthorizeDialog(onReauthorize = {}, onDismiss = {})
}
