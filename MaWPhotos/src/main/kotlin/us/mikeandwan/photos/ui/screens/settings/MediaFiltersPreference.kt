package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaType

/**
 * What a Filters button offers, for the area this sits under. The same saved values the button reads
 * and writes, so changing either one changes both.
 */
@Composable
fun MediaFiltersPreference(
    favoritesLabelStringId: Int,
    filters: MediaFilters,
    onChange: (MediaFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = listOf(
        stringResource(id = R.string.filter_type_all) to null,
        stringResource(id = R.string.filter_type_photos) to MediaType.Photo,
        stringResource(id = R.string.filter_type_videos) to MediaType.Video,
    )

    Column(modifier = modifier) {
        SwitchPreference(
            labelStringId = favoritesLabelStringId,
            isChecked = filters.favoritesOnly,
            onChange = { onChange(filters.copy(favoritesOnly = it)) },
        )
        SegmentedPreference(
            labelStringId = R.string.pref_filter_media_type,
            options = types.map { (label, _) -> label },
            selectedValue = types.first { (_, type) -> type == filters.mediaType }.first,
            onSelect = { selected ->
                onChange(filters.copy(mediaType = types.first { (label, _) -> label == selected }.second))
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaFiltersPreferencePreview() {
    MaterialTheme {
        MediaFiltersPreference(
            favoritesLabelStringId = R.string.pref_filter_favorite_media,
            filters = MediaFilters(favoritesOnly = true, mediaType = MediaType.Photo),
            onChange = {},
        )
    }
}
