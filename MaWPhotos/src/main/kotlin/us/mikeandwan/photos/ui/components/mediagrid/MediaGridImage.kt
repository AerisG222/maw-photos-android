package us.mikeandwan.photos.ui.components.mediagrid

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.favorite.FavoriteIcon

private const val PRESSED_SCALE = 0.94f

@Composable
fun <T> MediaGridImage(
    item: MediaGridItem<T>,
    size: Dp,
    onSelectImage: (MediaGridItem<T>) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((MediaGridItem<T>) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "gridItemPressScale",
    )

    Box(
        modifier = modifier
            .height(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }.clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) { onSelectImage(item) },
    ) {
        AsyncImage(
            model = item.url,
            contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_broken_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = CircleShape,
                )
                .padding(end = 4.dp)
                .alpha(0.7f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.mediaTypes.contains(MediaType.Video)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_play_circle),
                    contentDescription = stringResource(
                        id = R.string.li_category_thumbnail_description,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp, 4.dp, 0.dp, 4.dp)
                        .size(16.dp),
                )
            }

            if (item.mediaTypes.contains(MediaType.Photo)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_camera),
                    contentDescription = stringResource(
                        id = R.string.li_category_thumbnail_description,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp, 4.dp, 0.dp, 4.dp)
                        .size(16.dp),
                )
            }
        }

        if (onToggleFavorite != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = CircleShape,
                    )
                    .clickable {
                        haptics.performHapticFeedback(
                            if (item.isFavorite) {
                                HapticFeedbackType.ToggleOff
                            } else {
                                HapticFeedbackType.ToggleOn
                            },
                        )
                        onToggleFavorite(item)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FavoriteIcon(
                    isFavorite = item.isFavorite,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MediaGridImagePreview() {
    MediaGridImage(
        item = MediaGridItem(
            id = kotlin.uuid.Uuid.random(),
            url = "",
            mediaTypes = listOf(MediaType.Photo, MediaType.Video),
            data = Unit,
        ),
        size = 120.dp,
        onSelectImage = {},
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MediaGridImageFavoritePreview() {
    MediaGridImage(
        item = MediaGridItem(
            id = kotlin.uuid.Uuid.random(),
            url = "",
            mediaTypes = listOf(MediaType.Photo, MediaType.Video),
            data = Unit,
            isFavorite = true,
        ),
        size = 120.dp,
        onSelectImage = {},
        onToggleFavorite = {},
    )
}
