package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaType

@Composable
fun <T> MediaGridImage(
    item: MediaGridItem<T>,
    size: Dp,
    onSelectImage: (MediaGridItem<T>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(size)
            .clickable { onSelectImage(item) },
    ) {
        AsyncImage(
            model = item.url,
            contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_broken_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        if (item.mediaTypes.contains(MediaType.Video)) {
            AsyncImage(
                model = R.drawable.ic_round_play_circle,
                contentDescription = stringResource(
                    id = R.string.li_category_thumbnail_description,
                ),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(16.dp),
            )
        }

        if (item.mediaTypes.contains(MediaType.Photo)) {
            AsyncImage(
                model = R.drawable.ic_round_camera,
                contentDescription = stringResource(
                    id = R.string.li_category_thumbnail_description,
                ),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(16.dp),
            )
        }
    }
}
