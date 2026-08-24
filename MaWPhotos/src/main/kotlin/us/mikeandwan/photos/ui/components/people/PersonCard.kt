package us.mikeandwan.photos.ui.components.people

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.ui.components.favorite.FavoriteIcon

private const val PRESSED_SCALE = 0.94f

/**
 * One person in the grid: their preferred face crop, and the two badges the preferences allow.
 *
 * The face crop is square, so the tile is too - the thumbnail sizes the rest of the app lays out by
 * set the edge here.
 *
 * The favorite heart is always offered, unlike the badges beside it. Marking somebody is the only
 * way to get the handful of people actually looked for to the top of this grid, so hiding it behind
 * a preference would hide the feature itself.
 */
@Composable
fun PersonCard(
    person: Person,
    size: Dp,
    showName: Boolean,
    showMediaCount: Boolean,
    onToggleFavorite: (Person) -> Unit,
    modifier: Modifier = Modifier,
    // null while there is nowhere for a person to lead, which leaves the card inert rather than
    // giving it press feedback for a tap that does nothing
    onSelect: ((Person) -> Unit)? = null,
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
        label = "personCardPressScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }.then(
                if (onSelect != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                    ) { onSelect(person) }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(modifier = Modifier.size(size)) {
            // the silhouette sits underneath rather than being handed to the image as a fallback,
            // so it shows through in both cases that leave nothing to draw: a person the pipeline
            // has published no crop for, and a crop this device cannot decode - face crops are avif,
            // which needs api 31, and below that this is the intended outcome rather than a failure
            PersonSilhouette(modifier = Modifier.matchParentSize())

            AsyncImage(
                model = person.preferredFaceUrl,
                contentDescription = stringResource(id = R.string.people_face_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = CircleShape,
                    ).clickable {
                        haptics.performHapticFeedback(
                            if (person.isFavorite) {
                                HapticFeedbackType.ToggleOff
                            } else {
                                HapticFeedbackType.ToggleOn
                            },
                        )
                        onToggleFavorite(person)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FavoriteIcon(
                    isFavorite = person.isFavorite,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp),
                )
            }

            if (showMediaCount) {
                Text(
                    text = person.mediaCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = CircleShape,
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        if (showName) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

// a tinted silhouette rather than a flat block, so a person with no face still reads as a person
@Composable
private fun PersonSilhouette(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxSize(0.6f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PersonCardPreview() {
    PersonCard(
        person = Person(
            id = Uuid.random(),
            name = "Alice Anderson",
            preferredFaceUrl = null,
            mediaCount = 42,
            isFavorite = true,
        ),
        size = 120.dp,
        showName = true,
        showMediaCount = true,
        onToggleFavorite = {},
        onSelect = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PersonCardNoBadgesPreview() {
    PersonCard(
        person = Person(
            id = Uuid.random(),
            name = "Bob",
            preferredFaceUrl = null,
            mediaCount = 3,
            isFavorite = false,
        ),
        size = 120.dp,
        showName = false,
        showMediaCount = false,
        onToggleFavorite = {},
    )
}
