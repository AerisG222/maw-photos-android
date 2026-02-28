package us.mikeandwan.photos.ui.components.logo

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R

@Composable
fun Logo(modifier: Modifier = Modifier) {
    AsyncImage(
        model = R.drawable.ic_launch,
        contentDescription = stringResource(id = R.string.logo_description),
        modifier = modifier.size(96.dp),
    )
}
