package us.mikeandwan.photos.ui.screens.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.PersonSort
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridSkeleton
import us.mikeandwan.photos.ui.components.mediagrid.getSize
import us.mikeandwan.photos.ui.components.people.PersonCard

// Stable selector for UI automation (baseline profile generation). Surfaced to UiAutomator via
// `testTagsAsResourceId` enabled at the app root. Keep in sync with the matching literal in the
// :baselineprofile module's BaselineProfileGenerator.
const val PEOPLE_GRID_TAG = "peopleGrid"

@Composable
fun PeopleScreen(
    uiState: PeopleUiState,
    onFilterChange: (String) -> Unit,
    onToggleSort: () -> Unit,
    onToggleFavorite: (Person) -> Unit,
    modifier: Modifier = Modifier,
    // null until there is a person feed to open - see PersonCard
    onSelectPerson: ((Person) -> Unit)? = null,
) {
    if (uiState.isLoading) {
        MediaGridSkeleton(
            thumbnailSize = uiState.preferences.gridThumbnailSize,
            modifier = modifier,
        )

        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // the whole list is already in hand, so this narrows what is on screen without a round trip
        FilterBar(
            filter = uiState.filter,
            sortBy = uiState.preferences.sortBy,
            onFilterChange = onFilterChange,
            onToggleSort = onToggleSort,
        )

        when {
            !uiState.hasAnyPeople -> {
                Message(stringResource(id = R.string.people_none_found))
            }

            uiState.people.isEmpty() -> {
                Message(stringResource(id = R.string.people_no_matches))
            }

            else -> {
                val size = getSize(uiState.preferences.gridThumbnailSize)

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = size),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(PEOPLE_GRID_TAG),
                ) {
                    items(
                        uiState.people,
                        key = { person -> person.id },
                    ) { person ->
                        PersonCard(
                            person = person,
                            size = size,
                            showName = uiState.preferences.showNames,
                            showMediaCount = uiState.preferences.showMediaCounts,
                            onToggleFavorite = onToggleFavorite,
                            onSelect = onSelectPerson,
                            // favoriting reorders the grid, which is the point of the mark - let the
                            // card slide to its new place rather than jump
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    filter: String,
    sortBy: PersonSort,
    onFilterChange: (String) -> Unit,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // the button shows what the ordering currently is rather than what tapping would switch it to,
    // which is the only reading that makes sense when the grid beside it is already in that order
    val sortDescription = when (sortBy) {
        PersonSort.Name -> stringResource(id = R.string.people_sort_by_name)
        PersonSort.MediaCount -> stringResource(id = R.string.people_sort_by_media_count)
    }

    val sortIconId = when (sortBy) {
        PersonSort.Name -> R.drawable.ic_sort_by_alpha
        PersonSort.MediaCount -> R.drawable.ic_sort_by_count
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        TextField(
            value = filter,
            onValueChange = onFilterChange,
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.people_filter_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onToggleSort) {
            Icon(
                painter = painterResource(id = sortIconId),
                contentDescription = sortDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PeopleScreenPreview() {
    PeopleScreen(
        uiState = PeopleUiState(
            people = listOf(
                Person(Uuid.random(), "Alice Anderson", null, 42, true),
                Person(Uuid.random(), "Bob Brown", null, 17, false),
                Person(Uuid.random(), "Carol Clark", null, 3, false),
            ),
            preferences = PeoplePreference(gridThumbnailSize = GridThumbnailSize.Medium),
            isLoading = false,
            hasAnyPeople = true,
        ),
        onFilterChange = {},
        onToggleSort = {},
        onToggleFavorite = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PeopleScreenNoMatchesPreview() {
    PeopleScreen(
        uiState = PeopleUiState(
            people = emptyList(),
            filter = "zzz",
            isLoading = false,
            hasAnyPeople = true,
        ),
        onFilterChange = {},
        onToggleSort = {},
        onToggleFavorite = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PeopleScreenEmptyLibraryPreview() {
    PeopleScreen(
        uiState = PeopleUiState(isLoading = false, hasAnyPeople = false),
        onFilterChange = {},
        onToggleSort = {},
        onToggleFavorite = {},
    )
}
