package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media

class RandomMediaRepository
    @Inject
    constructor(
        private val api: MediaApiClient,
        randomPreferenceRepository: RandomPreferenceRepository,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            const val ERR_MSG_FETCH = "Unable to fetch random photos at this time.  Please try again later."
        }

        private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var periodicJob: PeriodicJob<ExternalCallStatus<List<Media>>>

        private val _media = MutableStateFlow(emptyList<Media>())
        val media = _media.asStateFlow()

        fun setDoFetch(doFetch: Boolean) {
            if (doFetch) {
                periodicJob.start()
            } else {
                periodicJob.stop()
            }
        }

        fun fetch(count: Int) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getRandomMedia(count)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_FETCH))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_FETCH))
                    }

                    is ApiResult.Success -> {
                        val newPhotos = result.result.map { it.toDomainMedia() }

                        _media.value += newPhotos

                        emit(ExternalCallStatus.Success(newPhotos))
                    }
                }
            }

        fun clear() {
            _media.value = emptyList()
        }

        init {
            periodicJob = PeriodicJob(
                false,
                3000L, // Initial default
            ) { fetch(1) }

            scope.launch {
                randomPreferenceRepository.getSlideshowIntervalSeconds().collect {
                    periodicJob.setIntervalMillis(it * 1000L)
                }
            }
        }
    }
