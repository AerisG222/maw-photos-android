package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaFaces
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.people.PersonFace

private val FACE_SIZE = 48.dp

/**
 * Who is in the photograph, each of them once, leading to everything else they appear in.
 *
 * Faces the app cannot put a name to are counted at the foot rather than left out. They are the
 * whole reason the count is here: a list of two names with nothing else said would read as the
 * complete answer when a third person is standing right there unrecognised.
 */
@Composable
fun WhoScreen(
    whoState: WhoState,
    modifier: Modifier = Modifier,
) {
    val faces = whoState.faces

    if (faces == null) {
        Loading(modifier = modifier)
        return
    }

    if (faces.people.isEmpty() && faces.unnamedCount == 0) {
        DetailMessage(
            text = stringResource(id = R.string.media_detail_who_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(modifier.then(Modifier.fillMaxSize())) {
        items(faces.people, key = { it.id }) { person ->
            PersonRow(
                person = person,
                onSelect = { whoState.onSelectPerson(person.id) },
            )
        }

        if (faces.unnamedCount > 0) {
            item {
                DetailMessage(
                    text = pluralStringResource(
                        id = R.plurals.media_detail_unnamed_faces,
                        count = faces.unnamedCount,
                        faces.unnamedCount,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PersonRow(
    person: Person,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PersonFace(
            person = person,
            shape = CircleShape,
            modifier = Modifier.size(FACE_SIZE),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // their whole library rather than this photograph, because that is what the row opens
            Text(
                text = pluralStringResource(
                    id = R.plurals.media_detail_person_media_count,
                    count = person.mediaCount,
                    person.mediaCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WhoScreenPreview() {
    WhoScreen(
        whoState = WhoState(
            faces = MediaFaces(
                people = listOf(
                    Person(Uuid.random(), "Alice Anderson", null, 412, false),
                    Person(Uuid.random(), "Bob Brown", null, 1, true),
                ),
                unnamedCount = 2,
            ),
            fetchFaces = {},
            onSelectPerson = {},
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun WhoScreenEmptyPreview() {
    WhoScreen(
        whoState = WhoState(
            faces = MediaFaces(),
            fetchFaces = {},
            onSelectPerson = {},
        ),
    )
}
