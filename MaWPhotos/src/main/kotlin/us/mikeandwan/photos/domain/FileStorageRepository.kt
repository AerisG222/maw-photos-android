package us.mikeandwan.photos.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import us.mikeandwan.photos.utils.getFilenameFromUrl

@Singleton
class FileStorageRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val imageLoader: ImageLoader,
        private val httpClient: OkHttpClient,
    ) {
        companion object {
            private val mimeTypeMap = MimeTypeMap.getSingleton()!!
            private const val DIR_SHARE = "photos_to_share"
            private const val DIR_UPLOAD = "upload"

            // high enough that the conversion is not what anybody notices about a shared photo
            private const val JPEG_QUALITY = 95
            private val JPEG_EXTENSIONS = setOf("jpg", "jpeg")
        }

        private val _pendingUploads = MutableStateFlow<List<File>>(emptyList())
        val pendingUploads = _pendingUploads.asStateFlow()

        /**
         * Puts a copy of the file at [url] where it can be handed to another app.
         *
         * A jpeg goes across as the server's own bytes - full quality, metadata and all - rather
         * than a re-encoding of what was drawn.  Anything else is converted to jpeg first: the app
         * on the other end is anybody's guess, and avif in particular is still turned away by mms,
         * by a number of messaging apps, and by android before 11.  That conversion loses the
         * metadata, which is no worse than every share used to be.
         *
         * The pager has almost always just shown the photo, so the image cache usually has it
         * already; only when it has been evicted, or was never cached, is it fetched again.
         */
        suspend fun saveMediaToShare(url: String): File =
            withContext(Dispatchers.IO) {
                val original = getShareFile(getFilenameFromUrl(url))

                try {
                    if (!copyFromImageCache(url, original)) {
                        download(url, original)
                    }

                    if (isJpeg(original)) original else convertToJpeg(original)
                } catch (e: Exception) {
                    // a half-written file would otherwise be offered up the next time the same
                    // photo is shared
                    original.delete()
                    throw e
                }
            }

        suspend fun saveFileToUpload(mediaUri: Uri): File? {
            val mimeType = context.contentResolver.getType(mediaUri)

            return if (isValidType(mimeType)) {
                writeUploadFile(mediaUri, mimeType!!)
            } else {
                null
            }
        }

        suspend fun clearShareCache() {
            withContext(Dispatchers.IO) {
                getShareDirectory()
                    ?.walkBottomUp()
                    ?.forEach { it.delete() }
            }
        }

        suspend fun clearImageCache() {
            withContext(Dispatchers.IO) {
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            }
        }

        suspend fun clearLegacyFiles() {
            withContext(Dispatchers.IO) {
                context
                    .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?.walkBottomUp()
                    ?.forEach { it.delete() }
            }
        }

        suspend fun clearLegacyDatabase() {
            withContext(Dispatchers.IO) {
                context.deleteDatabase("maw")
            }
        }

        suspend fun refreshPendingUploads() {
            withContext(Dispatchers.IO) {
                val uploads = getUploadDirectory()?.listFiles { it.isFile }?.asList() ?: emptyList()
                _pendingUploads.update { uploads }
            }
        }

        private suspend fun writeUploadFile(
            mediaUri: Uri,
            mimeType: String,
        ): File? =
            withContext(Dispatchers.IO) {
                val uploadFile = getUploadFile(mediaUri, mimeType)

                if (uploadFile.exists()) {
                    null
                } else {
                    context.contentResolver.openInputStream(mediaUri).use { inputStream ->
                        uploadFile.outputStream().use { outputStream ->
                            if (inputStream != null) {
                                inputStream.copyTo(outputStream)

                                uploadFile
                            } else {
                                null
                            }
                        }
                    }
                }
            }

        private fun getUploadFile(
            mediaUri: Uri,
            mimeType: String,
        ): File {
            val extension = mimeTypeMap.getExtensionFromMimeType(mimeType)
            val typeName = mimeType.substringBefore('/')
            val filename = "${typeName}_${mediaUri.lastPathSegment}.$extension"
            val dir = getUploadDirectory()

            dir?.mkdirs()

            return File(dir, filename)
        }

        // coil keys what it fetched by the url, and keeps the response body exactly as it arrived
        private fun copyFromImageCache(
            url: String,
            target: File,
        ): Boolean {
            val diskCache = imageLoader.diskCache ?: return false
            val snapshot = diskCache.openSnapshot(url) ?: return false

            snapshot.use {
                diskCache.fileSystem.source(it.data).buffer().use { source ->
                    target.sink().buffer().use { sink -> sink.writeAll(source) }
                }
            }

            return true
        }

        private fun isJpeg(file: File) = file.extension.lowercase() in JPEG_EXTENSIONS

    // the original is only ever an intermediate here, so it goes whether or not this succeeds
    private fun convertToJpeg(source: File): File {
        val target = File(source.parentFile, "${source.nameWithoutExtension}.jpg")

        try {
            // null for anything the platform cannot decode - avif below android 12 among them
            val bitmap = BitmapFactory.decodeFile(source.path)
                ?: throw IOException("Unable to decode ${source.name}")

            try {
                target.outputStream().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        throw IOException("Unable to convert ${source.name} to jpeg")
                    }
                }
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            source.delete()
        }

        return target
    }

    private fun download(
        url: String,
        target: File,
    ) {
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unable to download $url: HTTP ${response.code}")
            }

            target.outputStream().use { response.body.byteStream().copyTo(it) }
        }
    }

        private fun getUploadDirectory(): File? = context.getExternalFilesDir(DIR_UPLOAD)

        private fun getShareFile(originalFilename: String): File = File(getShareDirectory(), originalFilename)

        private fun getShareDirectory(): File? = context.getExternalFilesDir(DIR_SHARE)

        private fun isValidType(mimeType: String?): Boolean =
            mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))
    }
