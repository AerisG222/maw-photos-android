package us.mikeandwan.photos.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import us.mikeandwan.photos.R
import us.mikeandwan.photos.api.UploadApiClient
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.NotificationIdRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.ui.main.MainActivity
import us.mikeandwan.photos.utils.NOTIFICATION_CHANNEL_ID_UPLOAD_FILES
import us.mikeandwan.photos.utils.PendingIntentFlagHelper
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val apiClient: UploadApiClient,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val notificationManager: NotificationManager,
    private val fileStorageRepository: FileStorageRepository,
    private val notificationIdRepository: NotificationIdRepository
): CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_FILENAME = "filename"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val MAX_RETRIES = 8
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val file = inputData.getString(KEY_FILENAME)
        val fileToUpload = getValidatedFile(file)
            ?: return@withContext Result.failure(
                workDataOf(KEY_FAILURE_REASON to "invalid file: $file")
            )

        try {
            apiClient.uploadFile(fileToUpload)
            fileToUpload.delete()
            showNotification(true)
            fileStorageRepository.refreshPendingUploads()

            Result.success()
        } catch(error: Throwable) {
            if(runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                showNotification(false)
                fileToUpload.delete()
                fileStorageRepository.refreshPendingUploads()

                Result.failure(
                    workDataOf(KEY_FAILURE_REASON to error.message)
                )
            }
        }
    }

    private fun getValidatedFile(filename: String?): File? {
        if (filename.isNullOrBlank() || !File(filename).exists()) {
            return null
        }

        return File(filename)
    }

    private suspend fun showNotification(wasSuccessful: Boolean) {
        val i = Intent(applicationContext, MainActivity::class.java)
        val pendingIntentFlag = PendingIntentFlagHelper.getMutableFlag(PendingIntent.FLAG_UPDATE_CURRENT)
        val detailsIntent = PendingIntent.getActivity(applicationContext, 0, i, pendingIntentFlag)

        val title = if(wasSuccessful) "Media Uploaded!" else "Upload Failed"
        val msg = if(wasSuccessful) "File uploaded." else "File failed to upload after multiple attempts.  Please try again later."

        val builder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID_UPLOAD_FILES)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(detailsIntent)
                .setAutoCancel(true)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)

        if (preferenceRepository.getDoVibrate().first()) {
            builder.setVibrate(longArrayOf(300, 300))
        }

        val notification = builder.build()

        notificationManager.notify(notificationIdRepository.getAndInc(), notification)
    }
}
