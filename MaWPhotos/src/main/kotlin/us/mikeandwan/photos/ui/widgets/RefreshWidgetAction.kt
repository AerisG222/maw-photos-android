package us.mikeandwan.photos.ui.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.services.WidgetRandomPhotoService

class RefreshWidgetAction : ActionCallback {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RefreshWidgetEntryPoint {
        fun widgetRandomPhotoService(): WidgetRandomPhotoService
        fun errorRepository(): ErrorRepository
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                RefreshWidgetEntryPoint::class.java,
            )

        val service = entryPoint.widgetRandomPhotoService()
        val errorRepository = entryPoint.errorRepository()

        try {
            service.fetchAndRefreshWidget(context, glanceId)
        } catch (e: Exception) {
            errorRepository.logError("RefreshWidgetAction: Exception in onAction for $glanceId", e)
        }
    }
}
