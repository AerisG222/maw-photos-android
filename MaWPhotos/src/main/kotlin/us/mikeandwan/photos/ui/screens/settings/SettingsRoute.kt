package us.mikeandwan.photos.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState
import us.mikeandwan.photos.ui.shared.areNotificationsPermitted

@Serializable
object SettingsNavKey : NavKey

fun EntryProviderScope<NavKey>.settings() {
    entry<SettingsNavKey> {
        SettingsRoute()
    }
}

@Composable
private fun SettingsRoute(vm: SettingsViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val context = LocalContext.current
    val permissionPostNotificationAllowed = context.areNotificationsPermitted()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                vm.setNotificationDoNotify(true)
            }
        }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Settings)
        appActions.updateTopBar(
            NavigationArea.Settings,
            TopBarState(
                showAppIcon = false,
                title = "Settings",
            ),
        )
    }

    SettingsScreen(
        uiState = uiState,
        permissionPostNotificationAllowed = permissionPostNotificationAllowed,
        onNotificationDoNotifyChange = { doNotify ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && doNotify &&
                !context.areNotificationsPermitted()
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.setNotificationDoNotify(doNotify)
            }
        },
        onNotificationDoVibrateChange = { vm.setNotificationDoVibrate(it) },
        onCategoryDisplayTypeChange = { vm.setCategoryDisplayType(it) },
        onCategoryThumbnailSizeChange = { vm.setCategoryThumbnailSize(it) },
        onCategoryShowMediaTypeIndicatorChange = { vm.setCategoryShowMediaTypeIndicator(it) },
        onPhotoSlideshowIntervalChange = { vm.setPhotoSlideshowInterval(it) },
        onPhotoThumbnailSizeChange = { vm.setPhotoThumbnailSize(it) },
        onPhotoShowMediaTypeIndicatorChange = { vm.setPhotoShowMediaTypeIndicator(it) },
        onRandomSlideshowIntervalChange = { vm.setRandomSlideshowInterval(it) },
        onRandomThumbnailSizeChange = { vm.setRandomThumbnailSize(it) },
        onRandomShowMediaTypeIndicatorChange = { vm.setRandomShowMediaTypeIndicator(it) },
        onSearchQueryCountChange = { vm.setSearchQueryCount(it) },
        onSearchDisplayTypeChange = { vm.setSearchDisplayType(it) },
        onSearchThumbnailSizeChange = { vm.setSearchThumbnailSize(it) },
        onSearchShowMediaTypeIndicatorChange = { vm.setSearchShowMediaTypeIndicator(it) },
        onToggleDeveloperMode = { vm.toggleDeveloperMode(it) },
        onClearLogs = { vm.clearLogs() },
        onLogout = {
            vm.logout(context)
            appActions.navigateToLogin()
        },
    )
}
