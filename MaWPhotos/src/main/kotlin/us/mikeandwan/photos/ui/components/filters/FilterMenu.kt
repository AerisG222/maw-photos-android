package us.mikeandwan.photos.ui.components.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaType

/**
 * The one Filters button, showing how many are on, wherever a listing can be narrowed.
 *
 * The menu stays open as options change, so several can be set in one go with the listing updating
 * behind it.
 */
@Composable
fun FilterMenu(
    filters: MediaFilters,
    onChange: (MediaFilters) -> Unit,
    modifier: Modifier = Modifier,
    // not every listing has types to choose between - a person is not a photo or a video
    showMediaType: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            BadgedBox(
                badge = {
                    if (filters.isActive) {
                        Badge { Text(text = filters.activeCount.toString()) }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tune),
                    contentDescription = stringResource(id = R.string.filters),
                    tint = if (filters.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FilterOptions(filters, onChange, showMediaType)
        }
    }
}

@Composable
private fun FilterOptions(
    filters: MediaFilters,
    onChange: (MediaFilters) -> Unit,
    showMediaType: Boolean,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = filters.favoritesOnly,
                    role = Role.Checkbox,
                    onValueChange = { onChange(filters.copy(favoritesOnly = it)) },
                ).padding(end = 16.dp),
        ) {
            Checkbox(checked = filters.favoritesOnly, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(text = stringResource(id = R.string.filter_favorites))
        }

        if (showMediaType) {
            HorizontalDivider()

            MediaTypeOption(R.string.filter_type_all, null, filters, onChange)
            MediaTypeOption(R.string.filter_type_photos, MediaType.Photo, filters, onChange)
            MediaTypeOption(R.string.filter_type_videos, MediaType.Video, filters, onChange)
        }

        if (filters.isActive) {
            HorizontalDivider()

            TextButton(onClick = { onChange(MediaFilters()) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.filter_clear))
            }
        }
    }
}

@Composable
private fun MediaTypeOption(
    labelId: Int,
    mediaType: MediaType?,
    filters: MediaFilters,
    onChange: (MediaFilters) -> Unit,
) {
    val selected = filters.mediaType == mediaType

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onChange(filters.copy(mediaType = mediaType)) },
            ).padding(end = 16.dp),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Text(text = stringResource(id = labelId))
    }
}

/**
 * What a listing says when its filters left nothing, told apart from having nothing at all and with
 * the way out right there - otherwise the list just looks empty.
 */
@Composable
fun NoFilterMatches(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.filter_no_matches),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )

        TextButton(onClick = onClear) {
            Text(text = stringResource(id = R.string.filter_clear))
        }
    }
}

/**
 * Returns to the top when [key] changes.  A list holds its place by item, so narrowing or widening
 * it would otherwise leave it parked wherever that item happens to sit in the new list.
 *
 * The first composition is left alone, so coming back to a listing keeps its place.  Only scroll
 * what is on screen: a lazy state that has never been laid out waits for that before it scrolls.
 */
@Composable
fun ScrollToTopOnChange(
    key: Any,
    scrollToTop: suspend () -> Unit,
) {
    var previous by remember { mutableStateOf(key) }

    LaunchedEffect(key) {
        val changed = previous != key

        previous = key

        if (changed) {
            scrollToTop()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterOptionsPreview() {
    MaterialTheme {
        FilterOptions(
            filters = MediaFilters(favoritesOnly = true, mediaType = MediaType.Photo),
            onChange = {},
            showMediaType = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFilterMatchesPreview() {
    MaterialTheme {
        NoFilterMatches(onClear = {})
    }
}
