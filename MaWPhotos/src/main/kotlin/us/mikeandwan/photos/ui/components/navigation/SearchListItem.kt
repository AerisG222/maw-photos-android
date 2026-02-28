package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SearchListItem(
    term: String,
    onTermSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(
            onClick = { onTermSelected(term) },
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = term,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
