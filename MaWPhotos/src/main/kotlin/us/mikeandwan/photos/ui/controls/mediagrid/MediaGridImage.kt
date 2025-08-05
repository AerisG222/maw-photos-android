package us.mikeandwan.photos.ui.controls.mediagrid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R

@Composable
fun <T> ImageGridImage(
    item: MediaGridItem<T>,
    size: Dp,
    onSelectImage: (MediaGridItem<T>) -> Unit,
    modifier: Modifier = Modifier
) {
//    val showVideoBadge = remember(item.data) {
//        when(item.data) {
//            is MediaCategory -> item.data.type == MediaType.Video
//            is Video -> true
//            else -> false
//        }
//    }

    Box {
        AsyncImage(
            model = item.url,
            contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_broken_image),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .height(size)
                .clickable {
                    onSelectImage(item)
                }
        )

//        if(showVideoBadge) {
//            AsyncImage(
//                model = R.drawable.mdi_video,
//                contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
//                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
//                modifier = modifier
//                    .align(Alignment.TopStart)
//                    .padding(2.dp)
//                    .size(16.dp)
//            )
//        }
    }
}
