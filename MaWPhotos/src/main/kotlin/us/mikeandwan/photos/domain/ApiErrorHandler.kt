package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.ui.shared.toExternalCallStatus

class ApiErrorHandler
    @Inject
    constructor(
        private val errorRepository: ErrorRepository,
    ) {
        suspend fun handleError(
            error: ApiResult.Error,
            message: String?,
        ): ExternalCallStatus<Nothing> {
            if (error.exception is CancellationException) {
                return error.toExternalCallStatus()
            }

            val detailedLog = "API Error: ${error.exception?.message ?: "Unknown error"}\nCode: ${error.errorCode}\nMessage: ${error.error}"
            errorRepository.logError(detailedLog, error.exception)

            if (error.isUnauthorized()) {
                Timber.i("ApiErrorHandler::handleError: Unauthorized")
            } else if (!message.isNullOrBlank()) {
                Timber.i("ApiErrorHandler::handleError: $message")
                errorRepository.showError(message)
            }

            return error.toExternalCallStatus()
        }

        suspend fun handleEmpty(
            empty: ApiResult.Empty,
            message: String? = null,
        ): ExternalCallStatus<Nothing> {
            val detailedLog = "API Empty Result"
            errorRepository.logError(detailedLog)

            if (!message.isNullOrBlank()) {
                errorRepository.showError(message)
            }

            return empty.toExternalCallStatus()
        }
    }
