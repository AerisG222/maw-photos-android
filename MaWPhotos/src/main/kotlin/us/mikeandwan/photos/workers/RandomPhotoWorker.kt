package us.mikeandwan.photos.workers

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.ui.widgets.RandomPhotoWidget

@HiltWorker
class RandomPhotoWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val randomMediaRepository: RandomMediaRepository,
        private val imageLoader: ImageLoader,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val WORK_NAME = "RandomPhotoWidgetWorker"
        }

        override suspend fun doWork(): Result {
            return try {
                val result = randomMediaRepository
                    .fetch(1)
                    .filterIsInstance<ExternalCallStatus.Success<List<us.mikeandwan.photos.domain.models.Media>>>()
                    .first()

                val media = result.result.firstOrNull() ?: return Result.failure()
                val mediaFile = media.files.firstOrNull { it.scale.code == "full-hd" }
                    ?: media.files.firstOrNull()
                    ?: return Result.failure()

                val request = ImageRequest
                    .Builder(applicationContext)
                    .data(mediaFile.path)
                    // .size(800, 800)
                    .build()

                val imageResult = imageLoader.execute(request)

                if (imageResult is SuccessResult) {
                    val uuid = Uuid.random()
                    val filename = "widget_photo_$uuid.jpg"
                    val file = File(applicationContext.cacheDir, filename)

                    file.outputStream().use { outputStream ->
                        val bitmap = (imageResult.image as? BitmapImage)?.bitmap
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }

                    // Clean up old widget images
                    applicationContext.cacheDir
                        .listFiles { f ->
                            f.name.startsWith("widget_photo_") && f.name != filename
                        }?.forEach { it.delete() }

                    val manager = GlanceAppWidgetManager(applicationContext)
                    val glanceIds = manager.getGlanceIds(RandomPhotoWidget::class.java)

                    glanceIds.forEach { glanceId ->
                        updateAppWidgetState(applicationContext, glanceId) { prefs ->
                            prefs[RandomPhotoWidget.IMAGE_URL_KEY] = file.absolutePath
                        }
                        RandomPhotoWidget().update(applicationContext, glanceId)
                    }
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
