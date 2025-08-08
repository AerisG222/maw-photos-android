package us.mikeandwan.photos.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.FileProvider
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.utils.getFilenameFromUrl
import java.io.File

suspend fun shareMedia(
    ctx: Context,
    saveMediaToShare: (drawable: Drawable, filename: String, onComplete: (File) -> Unit) -> Unit,
    media: Media
) {
    val drawable = getMediaToShare(ctx, media)

    saveMediaToShare(
        drawable,
        getFilenameFromUrl(media.getMediaUrl())
    ) { fileToShare ->
        val contentUri = FileProvider.getUriForFile(ctx, "${BuildConfig.APPLICATION_ID}.fileprovider", fileToShare)
        val sendIntent = Intent(Intent.ACTION_SEND)

        sendIntent.setDataAndType(contentUri, "image/*")
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri)

        val shareIntent = Intent.createChooser(sendIntent, null)

        ctx.startActivity(shareIntent)
    }
}

private suspend fun getMediaToShare(ctx: Context, media: Media): Drawable {
    return withContext(Dispatchers.IO) {
        val loader = SingletonImageLoader.get(ctx)
        val request = ImageRequest.Builder(ctx)
            .data(media.getMediaUrl())
            .allowHardware(false) // Disable hardware bitmaps.
            .build()

        val result = loader.execute(request)

        when (result) {
            is SuccessResult -> result.image.asDrawable(ctx.resources)
            else -> throw IllegalStateException("Failed to load media drawable: $result")
        }
    }
}


