package us.mikeandwan.photos.ui.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import us.mikeandwan.photos.workers.RandomPhotoWorker

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val workRequest = OneTimeWorkRequestBuilder<RandomPhotoWorker>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "RefreshRandomPhotoWidget",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }
}
