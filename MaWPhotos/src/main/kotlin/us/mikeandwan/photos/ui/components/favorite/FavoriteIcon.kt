package us.mikeandwan.photos.ui.components.favorite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import us.mikeandwan.photos.R

private const val UNFAVORITED_ALPHA = 0.7f
private const val POP_SCALE = 1.4f

/**
 * The heart used wherever something can be favorited.  Toggling pops the icon with a spring so the
 * change registers as a deliberate action rather than a silent swap of two drawables.
 */
@Composable
fun FavoriteIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val scale = remember { Animatable(1f) }

    // the pop belongs to the toggle, not to the item arriving on screen - which also covers a lazy
    // list reusing this slot for a different (already favorited) item as it scrolls
    var isInitialState by remember { mutableStateOf(true) }

    val alpha by animateFloatAsState(
        targetValue = if (isFavorite) 1f else UNFAVORITED_ALPHA,
        label = "favoriteAlpha",
    )

    LaunchedEffect(isFavorite) {
        if (!isInitialState) {
            scale.animateTo(POP_SCALE, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }

        isInitialState = false
    }

    Icon(
        painter = painterResource(
            id = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
        ),
        contentDescription = stringResource(id = R.string.toggle_favorite_icon_description),
        tint = tint,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha
        },
    )
}
