package us.mikeandwan.photos.ui.shared

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.domain.models.Media

fun shareMedia(
    ctx: Context,
    saveMediaToShare: (url: String, onComplete: (File) -> Unit) -> Unit,
    media: Media,
) {
    saveMediaToShare(media.getMediaUrl()) { fileToShare ->
        val contentUri = FileProvider.getUriForFile(
            ctx,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            fileToShare,
        )
        val sendIntent = Intent(Intent.ACTION_SEND)

        // the receiving app is told the type the file actually is rather than being left to work
        // out what an image/* holds
        sendIntent.setDataAndType(contentUri, getMimeType(fileToShare))
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri)

        val shareIntent = Intent.createChooser(sendIntent, null)

        ctx.startActivity(shareIntent)
    }
}

private fun getMimeType(file: File): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "image/*"
