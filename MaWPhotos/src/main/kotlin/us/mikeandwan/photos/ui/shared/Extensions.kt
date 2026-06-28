package us.mikeandwan.photos.ui.shared

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.findTeaserImage
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem

fun Media.toMediaGridItem(
    useLargeTeaser: Boolean,
    showMediaTypeIndicator: Boolean = true,
): MediaGridItem<Media> =
    MediaGridItem(
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        if (showMediaTypeIndicator) listOf(this.type) else emptyList(),
        this,
    )

val preferredMediaScales = arrayOf("qhd", "full-hd", "nhd", "qvg")

fun Media.getMediaUrl(): String {
    for (scale in preferredMediaScales) {
        this.files.find { it.scale.code == scale }?.let {
            return it.path
        }
    }

    return ""
}

fun Category.toMediaGridItem(
    useLargeTeaser: Boolean,
    showMediaTypeIndicator: Boolean = true,
): MediaGridItem<Category> =
    MediaGridItem(
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        if (showMediaTypeIndicator) this.mediaTypes else emptyList(),
        this,
    )

fun <T> ApiResult<T>.toExternalCallStatus(): ExternalCallStatus<T> =
    when (this) {
        is ApiResult.Success -> ExternalCallStatus.Success(this.result)
        is ApiResult.Empty -> ExternalCallStatus.Error("Unexpected result")
        is ApiResult.Error -> ExternalCallStatus.Error(this.error, this.exception)
    }

fun Context.areNotificationsPermitted(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
    return true
}

// android.permission.ACCESS_LOCAL_NETWORK is a runtime permission introduced with Local Network
// Protection in Android 16 (API 36). Referenced as a string literal so this compiles against the
// minSdk = 26 surface without a NewApi guard on the constant itself.
const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

fun Context.isLocalNetworkPermitted(): Boolean {
    if (Build.VERSION.SDK_INT >= 36) {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_LOCAL_NETWORK_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED
    }
    return true
}
