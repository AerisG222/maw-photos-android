package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

@Composable
fun RandomMenu(
    fetchRandomPhotos: (Int) -> Unit,
    clearRandomPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(
                onClick = { fetchRandomPhotos(10) },
            ) {
                Text(
                    text = stringResource(id = R.string.fetch_10_photos),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(16.dp, 0.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(
                onClick = { fetchRandomPhotos(20) },
            ) {
                Text(
                    text = stringResource(id = R.string.fetch_20_photos),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        OutlinedButton(
            onClick = { clearRandomPhotos() },
            modifier = Modifier
                .padding(16.dp, 4.dp, 16.dp, 16.dp)
                .fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = stringResource(id = R.string.clear_random_photos),
            )
        }
    }
}
