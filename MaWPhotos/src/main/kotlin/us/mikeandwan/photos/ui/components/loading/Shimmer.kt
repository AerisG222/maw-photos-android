package us.mikeandwan.photos.ui.components.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

private const val SWEEP_DURATION_MILLIS = 1400
private const val SWEEP_WIDTH = 500f

/**
 * A brush that sweeps a highlight across whatever it fills, used by the loading placeholders so a
 * screen that is still fetching looks like the content it is about to show rather than a spinner.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val offset by transition.animateFloat(
        initialValue = -SWEEP_WIDTH,
        targetValue = SWEEP_WIDTH * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.inverseOnSurface

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + SWEEP_WIDTH, 0f),
    )
}
