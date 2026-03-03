package us.mikeandwan.photos.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

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

    fun areNotificationsPermitted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    val (permissionPostNotificationAllowed, setPermissionPostNotificationAllowed) = remember {
        mutableStateOf(areNotificationsPermitted())
    }

    LaunchedEffect(Unit) {
        appActions.setNavArea(NavigationArea.Settings)
        appActions.updateTopBar(
            NavigationArea.Settings,
            TopBarState(showAppIcon = false, title = "Settings"),
        )
        setPermissionPostNotificationAllowed(areNotificationsPermitted())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (it) {
            setPermissionPostNotificationAllowed(true)
        } else {
            setPermissionPostNotificationAllowed(false)
            vm.showError("Please enable the Notification permission under Settings > Apps > Maw Photos")
        }
    }

    SettingsScreen(
        uiState = uiState,
        permissionPostNotificationAllowed = permissionPostNotificationAllowed,
        onNotificationDoNotifyChange = { doNotify ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && doNotify && !areNotificationsPermitted()) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.setNotificationDoNotify(doNotify)
            }
        },
        onNotificationDoVibrateChange = { vm.setNotificationDoVibrate(it) },
        onCategoryDisplayTypeChange = { vm.setCategoryDisplayType(it) },
        onCategoryThumbnailSizeChange = { vm.setCategoryThumbnailSize(it) },
        onPhotoSlideshowIntervalChange = { vm.setPhotoSlideshowInterval(it) },
        onPhotoThumbnailSizeChange = { vm.setPhotoThumbnailSize(it) },
        onRandomSlideshowIntervalChange = { vm.setRandomSlideshowInterval(it) },
        onRandomThumbnailSizeChange = { vm.setRandomThumbnailSize(it) },
        onSearchQueryCountChange = { vm.setSearchQueryCount(it) },
        onSearchDisplayTypeChange = { vm.setSearchDisplayType(it) },
        onSearchThumbnailSizeChange = { vm.setSearchThumbnailSize(it) },
        onLogout = {
            vm.logout(context)
            appActions.navigateToLogin()
        },
    )
}
