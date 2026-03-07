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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import us.mikeandwan.photos.domain.ErrorRepository
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
        private val errorRepository: ErrorRepository,
    ) {
        suspend fun fetchAndRefreshAllWidgets(context: Context) {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RandomPhotoWidget::class.java)

                if (glanceIds.isEmpty()) {
                    errorRepository.logError("WidgetRandomPhotoService: No widgets found to refresh")
                    return
                }

                val file = fetchRandomPhoto(context)
                if (file == null) {
                    errorRepository.logError("WidgetRandomPhotoService: Failed to fetch random photo for refresh all")
                    return
                }

                glanceIds.forEach { glanceId ->
                    updateWidgetState(context, glanceId, file)
                }
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Exception in fetchAndRefreshAllWidgets", e)
            }
        }

        suspend fun fetchAndRefreshWidget(
            context: Context,
            glanceId: GlanceId,
        ) {
            try {
                val file = fetchRandomPhoto(context)
                if (file == null) {
                    errorRepository.logError(
                        "WidgetRandomPhotoService: Failed to fetch random photo for widget $glanceId",
                    )
                    return
                }
                updateWidgetState(context, glanceId, file)
            } catch (e: Exception) {
                errorRepository.logError(
                    "WidgetRandomPhotoService: Exception in fetchAndRefreshWidget for $glanceId",
                    e,
                )
            }
        }

        private suspend fun updateWidgetState(
            context: Context,
            glanceId: GlanceId,
            file: File,
        ) {
            try {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[RandomPhotoWidget.IMAGE_URL_KEY] = file.absolutePath
                }
                RandomPhotoWidget().update(context, glanceId)
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Failed to update widget state for $glanceId", e)
            }
        }

        private suspend fun fetchRandomPhoto(context: Context): File? {
            return try {
                val result = randomMediaRepository
                    .fetch(1)
                    .filterIsInstance<ExternalCallStatus.Success<List<Media>>>()
                    .first()

                val media = result.result.firstOrNull() ?: run {
                    errorRepository.logError("WidgetRandomPhotoService: Random media fetch returned empty list")
                    return null
                }

                val mediaFile = media.files.firstOrNull { it.scale.code == "full-hd" }
                    ?: media.files.firstOrNull()
                    ?: run {
                        errorRepository.logError("WidgetRandomPhotoService: Media ${media.id} has no files")
                        return null
                    }

                val request = ImageRequest
                    .Builder(context)
                    .data(mediaFile.path)
                    .size(600, 600)
                    .build()

                val imageResult = imageLoader.execute(request)

                if (imageResult is SuccessResult) {
                    val uuid = Uuid.random()
                    val filename = "widget_photo_$uuid.jpg"
                    val file = File(context.cacheDir, filename)

                    file.outputStream().use { outputStream ->
                        val bitmap = (imageResult.image as? BitmapImage)?.bitmap
                        if (bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        } else {
                            errorRepository.logError(
                                "WidgetRandomPhotoService: Loaded image is not a bitmap for ${mediaFile.path}",
                            )
                            return null
                        }
                    }

                    // Clean up old widget images
                    context.cacheDir
                        .listFiles { f ->
                            f.name.startsWith("widget_photo_") && f.name != filename
                        }?.forEach { it.delete() }

                    file
                } else {
                    errorRepository.logError("WidgetRandomPhotoService: Image loading failed for ${mediaFile.path}")
                    null
                }
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Exception during random photo fetch", e)
                null
            }
        }
    }
