package us.mikeandwan.photos.ui.components.paging

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

/**
 * What a paged listing shows once a page has failed: being near the end is still true, so loading
 * waits here for a tap rather than asking again for as long as the server keeps failing.
 */
@Composable
fun LoadMoreFailed(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageId: Int = R.string.load_more_failed,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(id = messageId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )

        TextButton(onClick = onRetry) {
            Text(text = stringResource(id = R.string.load_more_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadMoreFailedPreview() {
    MaterialTheme {
        LoadMoreFailed(onRetry = {})
    }
}
