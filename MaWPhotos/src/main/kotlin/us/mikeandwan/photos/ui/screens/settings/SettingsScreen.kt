package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    permissionPostNotificationAllowed: Boolean,
    onNotificationDoNotifyChange: (Boolean) -> Unit,
    onNotificationDoVibrateChange: (Boolean) -> Unit,
    onCategoryDisplayTypeChange: (CategoryDisplayType) -> Unit,
    onCategoryThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onPhotoSlideshowIntervalChange: (Int) -> Unit,
    onPhotoThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onRandomSlideshowIntervalChange: (Int) -> Unit,
    onRandomThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onSearchQueryCountChange: (Int) -> Unit,
    onSearchDisplayTypeChange: (CategoryDisplayType) -> Unit,
    onSearchThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayTypeList = listOf("Grid", "List")
    val thumbnailSizeList = listOf("ExtraSmall", "Small", "Medium", "Large")
    val slideshowIntervalList = listOf("1s", "2s", "3s", "4s", "5s", "10s", "15s", "20s", "30s")

    val dividerModifier = Modifier.padding(0.dp, 24.dp, 0.dp, 0.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // --- NOTIFICATIONS ----
        Heading(stringId = R.string.pref_notifications_header)
        SwitchPreference(
            labelStringId = R.string.pref_notifications_new_message_title,
            isChecked = uiState.notificationDoNotify && permissionPostNotificationAllowed,
            onChange = onNotificationDoNotifyChange,
        )
        SwitchPreference(
            labelStringId = R.string.pref_notifications_vibrate,
            isChecked = uiState.notificationDoVibrate,
            onChange = onNotificationDoVibrateChange,
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- CATEGORY LIST ----
        Heading(stringId = R.string.pref_category_display_header)
        MenuPreference(
            labelStringId = R.string.pref_category_display_header,
            options = displayTypeList,
            selectedValue = uiState.categoryDisplayType.toString(),
            onSelect = {
                onCategoryDisplayTypeChange(enumValueOf(it))
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.categoryThumbnailSize.toString(),
            onSelect = {
                onCategoryThumbnailSizeChange(enumValueOf(it))
            },
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- CATEGORY / PHOTO ----
        Heading(stringId = R.string.pref_media_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervalList,
            selectedValue = "${uiState.photoSlideshowInterval}s",
            onSelect = {
                onPhotoSlideshowIntervalChange(it.substring(0, it.length - 1).toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.photoThumbnailSize.toString(),
            onSelect = {
                onPhotoThumbnailSizeChange(enumValueOf(it))
            },
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- RANDOM ----
        Heading(stringId = R.string.pref_random_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervalList,
            selectedValue = "${uiState.randomSlideshowInterval}s",
            onSelect = {
                onRandomSlideshowIntervalChange(it.substring(0, it.length - 1).toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.randomThumbnailSize.toString(),
            onSelect = {
                onRandomThumbnailSizeChange(enumValueOf(it))
            },
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- SEARCH ----
        Heading(stringId = R.string.pref_search_display_header)
        MenuPreference(
            labelStringId = R.string.pref_search_query_count_to_remember,
            options = listOf("5", "10", "20", "30", "50"),
            selectedValue = uiState.searchQueryCount.toString(),
            onSelect = {
                onSearchQueryCountChange(it.toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.pref_category_display_header,
            options = displayTypeList,
            selectedValue = uiState.searchDisplayType.toString(),
            onSelect = {
                onSearchDisplayTypeChange(enumValueOf(it))
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.searchThumbnailSize.toString(),
            onSelect = {
                onSearchThumbnailSizeChange(enumValueOf(it))
            },
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- ADVANCED ----
        Heading(stringId = R.string.pref_advanced_display_header)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonColors(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                AsyncImage(
                    model = R.drawable.ic_logout,
                    contentDescription = stringResource(id = R.string.fragment_settings_log_out),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(4.dp, 4.dp, 12.dp, 4.dp)
                        .height(24.dp)
                        .width(24.dp),
                )

                Text(
                    text = stringResource(id = R.string.fragment_settings_log_out),
                    modifier = Modifier.padding(0.dp, 4.dp, 4.dp, 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        uiState = SettingsUiState(),
        permissionPostNotificationAllowed = true,
        onNotificationDoNotifyChange = {},
        onNotificationDoVibrateChange = {},
        onCategoryDisplayTypeChange = {},
        onCategoryThumbnailSizeChange = {},
        onPhotoSlideshowIntervalChange = {},
        onPhotoThumbnailSizeChange = {},
        onRandomSlideshowIntervalChange = {},
        onRandomThumbnailSizeChange = {},
        onSearchQueryCountChange = {},
        onSearchDisplayTypeChange = {},
        onSearchThumbnailSizeChange = {},
        onLogout = {}
    )
}
