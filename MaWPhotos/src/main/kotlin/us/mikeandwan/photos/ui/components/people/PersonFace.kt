package us.mikeandwan.photos.ui.components.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Person

/**
 * Somebody's preferred face crop, at whatever size and shape the caller lays out.
 *
 * The silhouette sits underneath the image rather than being handed to Coil as a fallback, so it
 * shows through in both cases that leave nothing to draw: a person the pipeline has published no
 * crop for, and a crop this device cannot decode - face crops are avif, which needs api 31, and
 * below that this is the intended outcome rather than a failure.
 */
@Composable
fun PersonFace(
    person: Person,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxSize(0.6f),
        )

        AsyncImage(
            model = person.preferredFaceUrl,
            contentDescription = stringResource(id = R.string.people_face_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PersonFacePreview() {
    PersonFace(
        person = Person(Uuid.random(), "Alice", null, 4, false),
        modifier = Modifier.size(64.dp),
    )
}
