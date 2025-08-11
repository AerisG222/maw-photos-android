package us.mikeandwan.photos.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.NotificationIdRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.ui.main.MainActivity
import us.mikeandwan.photos.utils.NOTIFICATION_CHANNEL_ID_NEW_CATEGORIES
import us.mikeandwan.photos.utils.PendingIntentFlagHelper

@HiltWorker
class UpdateCategoriesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val categoryRepository: CategoryRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val notificationManager: NotificationManager,
    private val notificationIdRepository: NotificationIdRepository
): CoroutineWorker(appContext, params) {
    companion object {
        const val WORK_NAME = "WORK: update_categories"

        private const val STATUS_UNKNOWN = 0
        private const val STATUS_SUCCESS = 1
        private const val STATUS_FAILED = 2
        private const val STATUS_RETRY = 3
    }

    override suspend fun doWork(): Result {
        if(categoryRepository.getYears().first().isEmpty()) {
            return Result.success()
        }

        var status = STATUS_UNKNOWN

        categoryRepository
            .getUpdatedCategories()
            .collect {
                when(it) {
                    is ExternalCallStatus.Loading -> { }
                    is ExternalCallStatus.Success -> {
                        status = STATUS_SUCCESS

                        if (it.result.isNotEmpty()) {
                            showSuccessNotification(it.result)
                        }
                    }
                    is ExternalCallStatus.Error -> {
                        if(runAttemptCount >= 3) {
                            status = STATUS_FAILED
                            showNotification("Error", "Unable to query for new categories at this time")
                        } else {
                            status = STATUS_RETRY
                        }
                    }
                }
            }

        return when(status) {
            STATUS_RETRY -> Result.retry()
            STATUS_FAILED -> Result.failure()
            else -> Result.success()
        }
    }

    private suspend fun showSuccessNotification(newCategories: List<Category>) {
        val pluralize = if (newCategories.size == 1) "category" else "categories"
        val contentText = "${newCategories.size} new $pluralize"

        showNotification("New Items Available", contentText)
    }

    private suspend fun showNotification(title: String, contentText: String) {
        val i = Intent(applicationContext, MainActivity::class.java)
        val pendingIntentFlag = PendingIntentFlagHelper.getMutableFlag(PendingIntent.FLAG_UPDATE_CURRENT)
        val detailsIntent = PendingIntent.getActivity(applicationContext, 0, i, pendingIntentFlag)

        val builder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID_NEW_CATEGORIES)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setContentTitle(title)
                .setContentText(contentText)
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
