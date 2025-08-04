package us.mikeandwan.photos.ui.controls.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PrimaryNavigationLink(
    iconId: Int,
    descriptionStringId: Int,
    isActiveArea: Boolean,
    onNavigate: () -> Unit,
    modifier: Modifier
) {
    val color = when(isActiveArea) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.onSurface
    }

    AsyncImage(
        model = iconId,
        contentDescription = stringResource(descriptionStringId),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .width(32.dp)
            .clickable { onNavigate() }
    )
}
