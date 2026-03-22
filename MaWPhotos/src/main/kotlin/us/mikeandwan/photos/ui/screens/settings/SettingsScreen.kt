package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.ui.components.logo.Logo

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    permissionPostNotificationAllowed: Boolean,
    onNotificationDoNotifyChange: (Boolean) -> Unit,
    onNotificationDoVibrateChange: (Boolean) -> Unit,
    onCategoryDisplayTypeChange: (CategoryDisplayType) -> Unit,
    onCategoryThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onCategoryShowMediaTypeIndicatorChange: (Boolean) -> Unit,
    onPhotoSlideshowIntervalChange: (Int) -> Unit,
    onPhotoThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onPhotoShowMediaTypeIndicatorChange: (Boolean) -> Unit,
    onRandomSlideshowIntervalChange: (Int) -> Unit,
    onRandomThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onRandomShowMediaTypeIndicatorChange: (Boolean) -> Unit,
    onSearchQueryCountChange: (Int) -> Unit,
    onSearchDisplayTypeChange: (CategoryDisplayType) -> Unit,
    onSearchThumbnailSizeChange: (GridThumbnailSize) -> Unit,
    onSearchShowMediaTypeIndicatorChange: (Boolean) -> Unit,
    onToggleDeveloperMode: (String) -> Unit,
    onClearLogs: () -> Unit,
    onLogout: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val displayTypeList = CategoryDisplayType.entries.map { it.name }
    val thumbnailSizeList = GridThumbnailSize.entries.map { it.name }
    val slideshowIntervals = listOf("1", "2", "3", "4", "5", "10", "15", "20", "25", "30", "45", "60")
    val searchCountList = listOf("5", "10", "20", "30", "50")
    val dividerModifier = Modifier.padding(vertical = 8.dp)
    var showDeveloperModeDialog by remember { mutableStateOf(false) }
    var iconClickCount by remember { mutableStateOf(0) }
    val tangerine = remember { FontFamily(Font(R.font.tangerine)) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
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

        // --- CATEGORIES ----
        Heading(stringId = R.string.pref_category_display_header)
        MenuPreference(
            labelStringId = R.string.display_type,
            options = displayTypeList,
            selectedValue = uiState.categoryDisplayType.name,
            onSelect = {
                onCategoryDisplayTypeChange(enumValueOf(it))
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.categoryThumbnailSize.name,
            onSelect = {
                onCategoryThumbnailSizeChange(enumValueOf(it))
            },
        )
        SwitchPreference(
            labelStringId = R.string.pref_show_media_type_indicator,
            isChecked = uiState.categoryShowMediaTypeIndicator,
            onChange = onCategoryShowMediaTypeIndicatorChange,
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- PHOTOS ----
        Heading(stringId = R.string.pref_media_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervals,
            selectedValue = uiState.photoSlideshowInterval.toString(),
            onSelect = {
                onPhotoSlideshowIntervalChange(it.toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.photoThumbnailSize.name,
            onSelect = {
                onPhotoThumbnailSizeChange(enumValueOf(it))
            },
        )
        SwitchPreference(
            labelStringId = R.string.pref_show_media_type_indicator,
            isChecked = uiState.photoShowMediaTypeIndicator,
            onChange = onPhotoShowMediaTypeIndicatorChange,
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- RANDOM ----
        Heading(stringId = R.string.pref_random_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervals,
            selectedValue = uiState.randomSlideshowInterval.toString(),
            onSelect = {
                onRandomSlideshowIntervalChange(it.toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.randomThumbnailSize.name,
            onSelect = {
                onRandomThumbnailSizeChange(enumValueOf(it))
            },
        )
        SwitchPreference(
            labelStringId = R.string.pref_show_media_type_indicator,
            isChecked = uiState.randomShowMediaTypeIndicator,
            onChange = onRandomShowMediaTypeIndicatorChange,
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- SEARCH ----
        Heading(stringId = R.string.pref_search_display_header)
        MenuPreference(
            labelStringId = R.string.pref_search_query_count_to_remember,
            options = searchCountList,
            selectedValue = uiState.searchQueryCount.toString(),
            onSelect = {
                onSearchQueryCountChange(it.toInt())
            },
        )
        MenuPreference(
            labelStringId = R.string.display_type,
            options = displayTypeList,
            selectedValue = uiState.searchDisplayType.name,
            onSelect = {
                onSearchDisplayTypeChange(enumValueOf(it))
            },
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = uiState.searchThumbnailSize.name,
            onSelect = {
                onSearchThumbnailSizeChange(enumValueOf(it))
            },
        )
        SwitchPreference(
            labelStringId = R.string.pref_show_media_type_indicator,
            isChecked = uiState.searchShowMediaTypeIndicator,
            onChange = onSearchShowMediaTypeIndicatorChange,
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- ADVANCED ----
        Heading(stringId = R.string.pref_advanced_display_header)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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

        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )

        // --- DEVELOPER LOGS (CONDITIONAL) ----
        if (uiState.isDeveloperMode) {
            DeveloperLogsScreen(
                logs = uiState.developerLogs,
                onClearLogs = onClearLogs,
                modifier = Modifier.height(400.dp),
            )
            HorizontalDivider(
                modifier = dividerModifier,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }

        // --- LOGO ----
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Logo(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    iconClickCount++
                    if (iconClickCount >= 4) {
                        showDeveloperModeDialog = true
                        iconClickCount = 0
                    }
                },
            )

            Text(
                text = "mikeandwan.us",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 48.sp,
                fontFamily = tangerine,
            )
            Text(
                text = "Photos v${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 48.sp,
                fontFamily = tangerine,
            )
        }
    }

    if (showDeveloperModeDialog) {
        DeveloperModeDialog(
            onDismiss = { showDeveloperModeDialog = false },
            onConfirm = {
                onToggleDeveloperMode(it)
                showDeveloperModeDialog = false
            },
        )
    }
}

@Composable
fun DeveloperModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developer Mode") },
        text = {
            Column {
                Text("Enter developer code:")
                TextField(
                    value = code,
                    onValueChange = { code = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(code) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
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
        onCategoryShowMediaTypeIndicatorChange = {},
        onPhotoSlideshowIntervalChange = {},
        onPhotoThumbnailSizeChange = {},
        onPhotoShowMediaTypeIndicatorChange = {},
        onRandomSlideshowIntervalChange = {},
        onRandomThumbnailSizeChange = {},
        onRandomShowMediaTypeIndicatorChange = {},
        onSearchQueryCountChange = {},
        onSearchDisplayTypeChange = {},
        onSearchThumbnailSizeChange = {},
        onSearchShowMediaTypeIndicatorChange = {},
        onToggleDeveloperMode = {},
        onClearLogs = {},
        onLogout = {},
    )
}
