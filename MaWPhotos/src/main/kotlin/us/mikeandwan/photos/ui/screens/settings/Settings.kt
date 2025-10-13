package us.mikeandwan.photos.ui.screens.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.controls.topbar.TopBarState

@Serializable
object SettingsRoute

fun NavGraphBuilder.settingsScreen(
    navigateToLogin: () -> Unit,
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit
) {
    composable<SettingsRoute> {
        val context = LocalContext.current

        val viewModel: SettingsViewModel = hiltViewModel()

        val notificationDoNotify by viewModel.notificationDoNotify.collectAsStateWithLifecycle()
        val notificationDoVibrate by viewModel.notificationDoVibrate.collectAsStateWithLifecycle()

        val categoryDisplayType by viewModel.categoryDisplayType.collectAsStateWithLifecycle()
        val categoryThumbnailSize by viewModel.categoryThumbnailSize.collectAsStateWithLifecycle()

        val photoSlideshowInterval by viewModel.photoSlideshowInterval.collectAsStateWithLifecycle()
        val photoThumbnailSize by viewModel.photoThumbnailSize.collectAsStateWithLifecycle()

        val randomSlideshowInterval by viewModel.randomSlideshowInterval.collectAsStateWithLifecycle()
        val randomThumbnailSize by viewModel.randomThumbnailSize.collectAsStateWithLifecycle()

        val searchQueryCount by viewModel.searchQueryCount.collectAsStateWithLifecycle()
        val searchDisplayType by viewModel.searchDisplayType.collectAsStateWithLifecycle()
        val searchThumbnailSize by viewModel.searchThumbnailSize.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            setNavArea(NavigationArea.Settings)
            updateTopBar(
                TopBarState().copy(
                    showAppIcon = false,
                    title = "Settings"
                )
            )
        }

        fun areNotificationsPermitted(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return  ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }

            return true
        }

        val (permissionPostNotificationAllowed, setPermissionPostNotificationAllowed) = mutableStateOf(areNotificationsPermitted())

        LaunchedEffect(Unit) {
            setPermissionPostNotificationAllowed(areNotificationsPermitted())
        }

        // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                setPermissionPostNotificationAllowed(true)
            } else {
                setPermissionPostNotificationAllowed(false)
                viewModel.showError("Please enable the Notification permission under Settings > Apps > Maw Photos")
            }
        }

        SettingsScreen(
            permissionPostNotificationAllowed,
            notificationDoNotify,
            notificationDoVibrate,
            categoryDisplayType,
            categoryThumbnailSize,
            photoSlideshowInterval,
            photoThumbnailSize,
            randomSlideshowInterval,
            randomThumbnailSize,
            searchQueryCount,
            searchDisplayType,
            searchThumbnailSize,
            setNotificationDoNotify = { doNotify ->
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    doNotify &&
                    !areNotificationsPermitted()
                ) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotificationDoNotify(doNotify)
                }
            },
            setNotificationDoVibrate = { viewModel.setNotificationDoVibrate(it) },
            setCategoryDisplayType = { viewModel.setCategoryDisplayType(it) },
            setCategoryThumbnailSize = { viewModel.setCategoryThumbnailSize(it) },
            setPhotoSlideshowInterval = { viewModel.setPhotoSlideshowInterval(it) },
            setPhotoThumbnailSize = { viewModel.setPhotoThumbnailSize(it) },
            setRandomSlideshowInterval = { viewModel.setRandomSlideshowInterval(it) },
            setRandomThumbnailSize = { viewModel.setRandomThumbnailSize(it) },
            setSearchQueryCount = { viewModel.setSearchQueryCount(it) },
            setSearchDisplayType = { viewModel.setSearchDisplayType(it) },
            setSearchThumbnailSize = { viewModel.setSearchThumbnailSize(it) },
            logout = {
                viewModel.logout(context)
                navigateToLogin()
            }
        )
    }
}

@Composable
fun SettingsScreen(
    permissionPostNotificationAllowed: Boolean,
    notificationDoNotify: Boolean,
    notificationDoVibrate: Boolean,
    categoryDisplayType: CategoryDisplayType,
    categoryThumbnailSize: GridThumbnailSize,
    photoSlideshowInterval: Int,
    photoThumbnailSize: GridThumbnailSize,
    randomSlideshowInterval: Int,
    randomThumbnailSize: GridThumbnailSize,
    searchQueryCount: Int,
    searchDisplayType: CategoryDisplayType,
    searchThumbnailSize: GridThumbnailSize,
    setNotificationDoNotify: (Boolean) -> Unit,
    setNotificationDoVibrate: (Boolean) -> Unit,
    setCategoryDisplayType: (CategoryDisplayType) -> Unit,
    setCategoryThumbnailSize: (GridThumbnailSize) -> Unit,
    setPhotoSlideshowInterval: (Int) -> Unit,
    setPhotoThumbnailSize: (GridThumbnailSize) -> Unit,
    setRandomSlideshowInterval: (Int) -> Unit,
    setRandomThumbnailSize: (GridThumbnailSize) -> Unit,
    setSearchQueryCount: (Int) -> Unit,
    setSearchDisplayType: (CategoryDisplayType) -> Unit,
    setSearchThumbnailSize: (GridThumbnailSize) -> Unit,
    logout: () -> Unit
) {
    val displayTypeList = listOf("Grid", "List")
    val thumbnailSizeList = listOf("ExtraSmall", "Small", "Medium", "Large")
    val slideshowIntervalList = listOf("1s", "2s", "3s", "4s", "5s", "10s", "15s", "20s", "30s")

    val dividerModifier = Modifier.padding(0.dp, 24.dp, 0.dp, 0.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- NOTIFICATIONS ----
        Heading(stringId = R.string.pref_notifications_header)
        SwitchPreference(
            labelStringId = R.string.pref_notifications_new_message_title,
            isChecked = notificationDoNotify && permissionPostNotificationAllowed,
            onChange = { setNotificationDoNotify(it) }
        )
        SwitchPreference(
            labelStringId = R.string.pref_notifications_vibrate,
            isChecked = notificationDoVibrate,
            onChange = { setNotificationDoVibrate(it) }
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )

        // --- CATEGORY LIST ----
        Heading(stringId = R.string.pref_category_display_header)
        MenuPreference(
            labelStringId = R.string.pref_category_display_header,
            options = displayTypeList,
            selectedValue = categoryDisplayType.toString(),
            onSelect = {
                setCategoryDisplayType(enumValueOf(it))
            }
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = categoryThumbnailSize.toString(),
            onSelect = {
                setCategoryThumbnailSize(enumValueOf(it))
            }
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )

        // --- CATEGORY / PHOTO ----
        Heading(stringId = R.string.pref_media_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervalList,
            selectedValue = "${photoSlideshowInterval}s",
            onSelect = {
                setPhotoSlideshowInterval(it.substring(0, it.length - 1).toInt())
            }
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = photoThumbnailSize.toString(),
            onSelect = {
                setPhotoThumbnailSize(enumValueOf(it))
            }
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )

        // --- RANDOM ----
        Heading(stringId = R.string.pref_random_display_header)
        MenuPreference(
            labelStringId = R.string.pref_media_display_slideshow_interval,
            options = slideshowIntervalList,
            selectedValue = "${randomSlideshowInterval}s",
            onSelect = {
                setRandomSlideshowInterval(it.substring(0, it.length - 1).toInt())
            }
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = randomThumbnailSize.toString(),
            onSelect = {
                setRandomThumbnailSize(enumValueOf(it))
            }
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )

        // --- SEARCH ----
        Heading(stringId = R.string.pref_search_display_header)
        MenuPreference(
            labelStringId = R.string.pref_search_query_count_to_remember,
            options = listOf("5", "10", "20", "30", "50"),
            selectedValue = searchQueryCount.toString(),
            onSelect = {
                setSearchQueryCount(it.toInt())
            }
        )
        MenuPreference(
            labelStringId = R.string.pref_category_display_header,
            options = displayTypeList,
            selectedValue = searchDisplayType.toString(),
            onSelect = {
                setSearchDisplayType(enumValueOf(it))
            }
        )
        MenuPreference(
            labelStringId = R.string.grid_thumbnail_size,
            options = thumbnailSizeList,
            selectedValue = searchThumbnailSize.toString(),
            onSelect = {
                setSearchThumbnailSize(enumValueOf(it))
            }
        )
        HorizontalDivider(
            modifier = dividerModifier,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )

        // --- ADVANCED ----
        Heading(stringId = R.string.pref_advanced_display_header)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            OutlinedButton(
                onClick = logout,
                colors = ButtonColors(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                AsyncImage(
                    model = R.drawable.ic_logout,
                    contentDescription = stringResource(id = R.string.fragment_settings_log_out),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(4.dp, 4.dp, 12.dp, 4.dp)
                        .height(24.dp)
                        .width(24.dp)
                )

                Text(
                    text = stringResource(id = R.string.fragment_settings_log_out),
                    modifier = Modifier.padding(0.dp, 4.dp, 4.dp, 4.dp)
                )
            }
        }
    }
}
