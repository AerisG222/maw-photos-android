package us.mikeandwan.photos.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FileStorageRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private val mimeTypeMap = MimeTypeMap.getSingleton()!!
        private const val DIR_SHARE = "photos_to_share"
        private const val DIR_UPLOAD = "upload"
    }

    private val _pendingUploads = MutableStateFlow<List<File>>(emptyList())
    val pendingUploads = _pendingUploads.asStateFlow()

    suspend fun savePhotoToShare(drawable: Drawable, originalFilename: String): File {
        return withContext(Dispatchers.IO) {
            val fileToShare = getShareFile(originalFilename)

            if(fileToShare.exists()) {
                // if there is an old share with the same name, it may not yet have been cleaned up yet
                fileToShare.delete()
            }

            fileToShare.outputStream().use { outputStream ->
                val bitmap = (drawable as BitmapDrawable).bitmap

                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)) {
                    throw Exception("failed to save drawable!")
                }
            }

            fileToShare
        }
    }

    suspend fun saveFileToUpload(mediaUri: Uri): File? {
        val mimeType = context.contentResolver.getType(mediaUri)

        return if(isValidType(mimeType)) {
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

    suspend fun clearLegacyFiles() {
        withContext(Dispatchers.IO) {
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
            _pendingUploads.value = getUploadDirectory()!!.listFiles { it.isFile }!!.asList()
        }
    }

    private suspend fun writeUploadFile(mediaUri: Uri, mimeType: String): File? {
        return withContext(Dispatchers.IO) {
            val uploadFile = getUploadFile(mediaUri, mimeType)

            if(uploadFile.exists()) {
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
    }

    private fun getUploadFile(mediaUri: Uri, mimeType: String): File {
        val extension = mimeTypeMap.getExtensionFromMimeType(mimeType)
        val typeName = mimeType.substringBefore('/')
        val filename = "${typeName}_${mediaUri.lastPathSegment}.${extension}"
        val dir = getUploadDirectory()

        dir?.mkdirs()

        return File(dir, filename)
    }

    private fun getUploadDirectory(): File? {
        return context.getExternalFilesDir(DIR_UPLOAD)
    }

    private fun getShareFile(originalFilename: String): File {
        return File(getShareDirectory(), originalFilename)
    }

    private fun getShareDirectory(): File? {
        return context.getExternalFilesDir(DIR_SHARE)
    }

    private fun isValidType(mimeType: String?): Boolean {
        return mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))
    }
}
