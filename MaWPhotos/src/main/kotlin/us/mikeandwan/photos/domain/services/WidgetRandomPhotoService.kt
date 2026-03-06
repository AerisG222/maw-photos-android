package us.mikeandwan.photos.domain.services

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.maxBitmapSize
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.widgets.RandomPhotoWidget

@Singleton
class WidgetRandomPhotoService
    @Inject
    constructor(
        private val randomMediaRepository: RandomMediaRepository,
        private val imageLoader: ImageLoader,
    ) {
        suspend fun fetchAndRefreshAllWidgets(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(RandomPhotoWidget::class.java)

            if (glanceIds.isEmpty()) return

            val file = fetchRandomPhoto(context) ?: return

            glanceIds.forEach { glanceId ->
                updateWidgetState(context, glanceId, file)
            }
        }

        suspend fun fetchAndRefreshWidget(
            context: Context,
            glanceId: GlanceId,
        ) {
            val file = fetchRandomPhoto(context) ?: return
            updateWidgetState(context, glanceId, file)
        }

        private suspend fun updateWidgetState(
            context: Context,
            glanceId: GlanceId,
            file: File,
        ) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[RandomPhotoWidget.IMAGE_URL_KEY] = file.absolutePath
            }
            RandomPhotoWidget().update(context, glanceId)
        }

        private suspend fun fetchRandomPhoto(context: Context): File? {
            return try {
                val result = randomMediaRepository
                    .fetch(1)
                    .filterIsInstance<ExternalCallStatus.Success<List<Media>>>()
                    .first()

                val media = result.result.firstOrNull() ?: return null
                val mediaFile = media.files.firstOrNull { it.scale.code == "full-hd" }
                    ?: media.files.firstOrNull()
                    ?: return null

                val request = ImageRequest
                    .Builder(context)
                    .data(mediaFile.path)
                    .size(800, 800)
                    .build()

                val imageResult = imageLoader.execute(request)

                if (imageResult is SuccessResult) {
                    val uuid = Uuid.random()
                    val filename = "widget_photo_$uuid.jpg"
                    val file = File(context.cacheDir, filename)

                    file.outputStream().use { outputStream ->
                        val bitmap = (imageResult.image as? BitmapImage)?.bitmap
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }

                    // Clean up old widget images
                    context.cacheDir
                        .listFiles { f ->
                            f.name.startsWith("widget_photo_") && f.name != filename
                        }?.forEach { it.delete() }

                    file
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
