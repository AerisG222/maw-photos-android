package us.mikeandwan.photos.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.services.WidgetRandomPhotoService

class RandomPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RandomPhotoWidget()

    private val scope = MainScope()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RandomPhotoWidgetReceiverEntryPoint {
        fun widgetRandomPhotoService(): WidgetRandomPhotoService

        fun errorRepository(): ErrorRepository
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                RandomPhotoWidgetReceiverEntryPoint::class.java,
            )

        val service = entryPoint.widgetRandomPhotoService()
        val errorRepository = entryPoint.errorRepository()

        // Trigger an immediate fetch when widgets are added or updated
        scope.launch {
            try {
                service.fetchAndRefreshAllWidgets(context)
            } catch (e: Exception) {
                errorRepository.logError("RandomPhotoWidgetReceiver: Exception in onUpdate scope", e)
            }
        }
    }
}
