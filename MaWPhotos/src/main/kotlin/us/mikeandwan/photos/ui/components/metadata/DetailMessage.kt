package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A sentence standing in for a card's contents.
 *
 * Every tab in the details sheet has something true to say when it has nothing to list - that the
 * media was never placed, that nobody was detected in it - and saying so is worth more than an
 * empty panel, which only ever reads as something having gone wrong.
 */
@Composable
fun DetailMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailMessagePreview() {
    DetailMessage(text = "We do not know where this was taken")
}
