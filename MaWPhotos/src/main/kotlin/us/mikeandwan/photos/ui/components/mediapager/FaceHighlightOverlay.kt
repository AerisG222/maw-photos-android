package us.mikeandwan.photos.ui.components.mediapager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.FaceHighlight

/**
 * Boxes over the faces detected in the photo underneath, labelled with whoever they belong to.
 *
 * [imageSize] is the photo's own pixel size rather than the space it is drawn in: face boxes are
 * normalised against the source frame, and ContentScale.Fit letterboxes, so the drawn rectangle has
 * to be recovered before a normalised box means anything on screen.  Whoever lays this out is
 * responsible for giving it exactly the same bounds the image was given.
 */
@Composable
fun FaceHighlightOverlay(
    faces: List<FaceHighlight>,
    imageSize: Size,
    modifier: Modifier = Modifier,
) {
    // a face the API will not name is drawn no differently by accident - the dashes say the app has
    // seen somebody here without claiming to know who, which is also what an unassigned face looks
    // like.  the two are indistinguishable by design; see FaceHighlight.
    val identifiedColor = MaterialTheme.colorScheme.primary
    val unidentifiedColor = MaterialTheme.colorScheme.outline
    val labelTextColor = MaterialTheme.colorScheme.onPrimary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium

    Canvas(modifier = modifier) {
        val frame = fitInside(imageSize, size) ?: return@Canvas

        faces.forEach { face ->
            // the edges are clamped rather than the origin and the extent, so a face the frame cuts
            // off is trimmed at the edge instead of being slid back inside it at full size - see
            // FaceHighlight, whose boxes are not promised to be within 0..1
            val left = face.boxX.coerceIn(0f, 1f)
            val top = face.boxY.coerceIn(0f, 1f)
            val right = (face.boxX + face.boxWidth).coerceIn(0f, 1f)
            val bottom = (face.boxY + face.boxHeight).coerceIn(0f, 1f)

            if (right <= left || bottom <= top) {
                return@forEach
            }

            val topLeft = Offset(
                x = frame.left + left * frame.width,
                y = frame.top + top * frame.height,
            )

            val boxSize = Size(
                width = (right - left) * frame.width,
                height = (bottom - top) * frame.height,
            )

            val cornerRadius = CornerRadius(4.dp.toPx())
            val isIdentified = face.personId != null

            // a dark halo underneath, because a single stroke in any one color disappears against
            // some photo somewhere
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 4.dp.toPx()),
            )

            drawRoundRect(
                color = if (isIdentified) identifiedColor else unidentifiedColor,
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = cornerRadius,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = if (isIdentified) {
                        null
                    } else {
                        PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                    },
                ),
            )

            face.name?.let { name ->
                drawNameLabel(
                    name = name,
                    boxTopLeft = topLeft,
                    boxSize = boxSize,
                    frame = frame,
                    textMeasurer = textMeasurer,
                    style = labelStyle.copy(color = labelTextColor),
                    chipColor = identifiedColor,
                )
            }
        }
    }
}

/**
 * Draws the name beneath its box, or above it when there is no room below.
 *
 * Kept inside the photo rather than inside the box: a name is almost always wider than the face it
 * belongs to, and a label clipped to the box would be unreadable on every small face in a group.
 */
private fun DrawScope.drawNameLabel(
    name: String,
    boxTopLeft: Offset,
    boxSize: Size,
    frame: FittedFrame,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    chipColor: Color,
) {
    val horizontalPadding = 6.dp.toPx()
    val verticalPadding = 3.dp.toPx()
    val gap = 3.dp.toPx()

    val measured = textMeasurer.measure(
        text = name,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        // a name longer than the photo is wide is cut rather than allowed to run off the edge
        constraints = Constraints(maxWidth = (frame.width - 2 * horizontalPadding).toInt().coerceAtLeast(0)),
    )

    val chipSize = Size(
        width = measured.size.width + 2 * horizontalPadding,
        height = measured.size.height + 2 * verticalPadding,
    )

    if (chipSize.width > frame.width) {
        return
    }

    // centred on the face, then pushed back inside the photo - a face near either edge would
    // otherwise hang its label over the letterboxing
    val chipX = (boxTopLeft.x + boxSize.width / 2f - chipSize.width / 2f)
        .coerceIn(frame.left, frame.left + frame.width - chipSize.width)

    val below = boxTopLeft.y + boxSize.height + gap
    val above = boxTopLeft.y - gap - chipSize.height
    val chipY = if (below + chipSize.height <= frame.top + frame.height) {
        below
    } else {
        above.coerceAtLeast(frame.top)
    }

    drawRoundRect(
        color = chipColor,
        topLeft = Offset(chipX, chipY),
        size = chipSize,
        cornerRadius = CornerRadius(3.dp.toPx()),
    )

    drawText(
        textLayoutResult = measured,
        topLeft = Offset(chipX + horizontalPadding, chipY + verticalPadding),
    )
}

/**
 * Where an image of [imageSize] actually lands inside [bounds] under ContentScale.Fit, or null when
 * there is nothing sensible to compute - an image whose size has not been read yet, or a container
 * that has not been measured.
 */
private fun fitInside(
    imageSize: Size,
    bounds: Size,
): FittedFrame? {
    if (!imageSize.isSpecified ||
        imageSize.width <= 0f ||
        imageSize.height <= 0f ||
        bounds.width <= 0f ||
        bounds.height <= 0f
    ) {
        return null
    }

    val scale = min(bounds.width / imageSize.width, bounds.height / imageSize.height)
    val width = imageSize.width * scale
    val height = imageSize.height * scale

    return FittedFrame(
        left = (bounds.width - width) / 2f,
        top = (bounds.height - height) / 2f,
        width = width,
        height = height,
    )
}

private data class FittedFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

@Preview(showBackground = true)
@Composable
private fun FaceHighlightOverlayPreview() {
    FaceHighlightOverlay(
        faces = listOf(
            FaceHighlight(Uuid.random(), Uuid.random(), "Mike Morano", 0.08f, 0.15f, 0.25f, 0.3f),
            FaceHighlight(Uuid.random(), Uuid.random(), "Wan Choi", 0.45f, 0.2f, 0.22f, 0.28f),
            // identified, but the people list has nobody by that id - drawn, just not labelled
            FaceHighlight(Uuid.random(), Uuid.random(), null, 0.75f, 0.3f, 0.18f, 0.22f),
            // nobody has been assigned to this one, or this caller may not know who it is
            FaceHighlight(Uuid.random(), null, null, 0.2f, 0.6f, 0.2f, 0.25f),
        ),
        imageSize = Size(1200f, 800f),
        modifier = Modifier.size(360.dp, 240.dp),
    )
}
