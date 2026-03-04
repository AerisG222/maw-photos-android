package us.mikeandwan.photos.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import us.mikeandwan.photos.workers.RandomPhotoWorker

class RandomPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RandomPhotoWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Trigger an immediate fetch when widgets are added or updated
        val workRequest = OneTimeWorkRequestBuilder<RandomPhotoWorker>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "RefreshRandomPhotoWidgetOnUpdate",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }
}
