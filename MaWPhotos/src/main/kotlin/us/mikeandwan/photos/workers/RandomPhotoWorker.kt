package us.mikeandwan.photos.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.services.WidgetRandomPhotoService

@HiltWorker
class RandomPhotoWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val widgetRandomPhotoService: WidgetRandomPhotoService,
        private val errorRepository: ErrorRepository,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val WORK_NAME = "RandomPhotoWidgetWorker"
        }

        override suspend fun doWork(): Result =
            try {
                widgetRandomPhotoService.fetchAndRefreshAllWidgets(applicationContext)
                Result.success()
            } catch (e: Exception) {
                errorRepository.logError("RandomPhotoWorker: Exception in doWork", e)
                Result.failure()
            }
    }
