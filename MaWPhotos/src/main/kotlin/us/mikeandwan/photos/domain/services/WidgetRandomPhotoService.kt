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
import kotlinx.coroutines.flow.first
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.ui.widgets.RandomPhotoWidget

@Singleton
class WidgetRandomPhotoService
    @Inject
    constructor(
        private val randomMediaRepository: RandomMediaRepository,
        private val categoryRepository: CategoryRepository,
        private val randomPreferenceRepository: RandomPreferenceRepository,
        private val imageLoader: ImageLoader,
        private val errorRepository: ErrorRepository,
    ) {
        suspend fun fetchAndRefreshAllWidgets(context: Context) {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RandomPhotoWidget::class.java)

                errorRepository.logInfo("calling fetchAndRefreshAllWidgets with ${glanceIds.size} widgets")

                if (glanceIds.isEmpty()) {
                    errorRepository.logError("WidgetRandomPhotoService: No widgets found to refresh")
                    return
                }

                val photoData = fetchRandomPhoto(context)
                if (photoData == null) {
                    errorRepository.logError("WidgetRandomPhotoService: Failed to fetch random photo for refresh all")
                    return
                }

                glanceIds.forEach { glanceId ->
                    updateWidgetState(context, glanceId, photoData)
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
                errorRepository.logInfo("calling fetchAndRefreshWidget for widget $glanceId")

                val photoData = fetchRandomPhoto(context)
                if (photoData == null) {
                    errorRepository.logError(
                        "WidgetRandomPhotoService: Failed to fetch random photo for widget $glanceId",
                    )
                    return
                }

                updateWidgetState(context, glanceId, photoData)
            } catch (e: Exception) {
                errorRepository.logError(
                    "WidgetRandomPhotoService: Exception in fetchAndRefreshWidget for $glanceId",
                    e,
                )
            }
        }

        suspend fun updateShowInfo(
            context: Context,
            showInfo: Boolean,
        ) {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RandomPhotoWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[RandomPhotoWidget.SHOW_INFO_KEY] = showInfo
                    }
                    RandomPhotoWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Failed to update showInfo for widgets", e)
            }
        }

        private suspend fun updateWidgetState(
            context: Context,
            glanceId: GlanceId,
            photoData: PhotoData,
        ) {
            try {
                errorRepository.logInfo("setting widget id $glanceId to use image ${photoData.file.absolutePath}")

                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[RandomPhotoWidget.IMAGE_URL_KEY] = photoData.file.absolutePath
                    prefs[RandomPhotoWidget.CATEGORY_NAME_KEY] = photoData.categoryName
                    prefs[RandomPhotoWidget.CATEGORY_YEAR_KEY] = photoData.categoryYear
                    prefs[RandomPhotoWidget.SHOW_INFO_KEY] = photoData.showInfo
                }

                RandomPhotoWidget().update(context, glanceId)

                errorRepository.logInfo("completed widget update for $glanceId")
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Failed to update widget state for $glanceId", e)
            }
        }

        private suspend fun fetchRandomPhoto(context: Context): PhotoData? {
            return try {
                val result = randomMediaRepository
                    .fetch(1)
                    .first { it !is ExternalCallStatus.Loading }

                if (result is ExternalCallStatus.Error) {
                    errorRepository.logError("WidgetRandomPhotoService: Repository returned error: ${result.message}")
                    return null
                }

                val successResult = result as? ExternalCallStatus.Success<List<Media>>
                val media = successResult?.result?.firstOrNull() ?: run {
                    errorRepository.logInfo(
                        "WidgetRandomPhotoService: Random media fetch returned empty list or unexpected status",
                    )
                    return null
                }

                val category = categoryRepository
                    .getCategory(media.categoryId)
                    .first { it != null } ?: run {
                    errorRepository.logError("WidgetRandomPhotoService: Failed to fetch category ${media.categoryId}")
                    return null
                }

                val prefs = randomPreferenceRepository.getRandomPreferences().first()

                val mediaFile = media.files.firstOrNull { it.scale.code == "full-hd" }
                    ?: media.files.firstOrNull()
                    ?: run {
                        errorRepository.logError("WidgetRandomPhotoService: Media ${media.id} has no files")
                        return null
                    }

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
                        if (bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                            outputStream.flush()
                        } else {
                            errorRepository.logError(
                                "WidgetRandomPhotoService: Loaded image is not a bitmap for ${mediaFile.path}",
                            )
                            return null
                        }
                    }

                    errorRepository.logInfo(
                        "successfully loaded new random image and saved to ${file.absolutePath} with size of ${file.length()}",
                    )

                    context.cacheDir
                        .listFiles { f ->
                            f.name.startsWith("widget_photo_") && f.name != filename
                        }?.forEach { it.delete() }

                    errorRepository.logInfo(
                        "does file exist after cleanup pass: ${file.exists()}",
                    )

                    PhotoData(
                        file = file,
                        categoryName = category.name,
                        categoryYear = category.year,
                        showInfo = prefs.showWidgetInfo,
                    )
                } else {
                    errorRepository.logError("WidgetRandomPhotoService: Image loading failed for ${mediaFile.path}")
                    null
                }
            } catch (e: Exception) {
                errorRepository.logError("WidgetRandomPhotoService: Exception during random photo fetch", e)
                null
            }
        }

        private data class PhotoData(
            val file: File,
            val categoryName: String,
            val categoryYear: Int,
            val showInfo: Boolean,
        )
    }
