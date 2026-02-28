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
        fun handleError(
            error: ApiResult.Error,
            message: String?,
        ): ExternalCallStatus<Nothing> {
            if (error.exception is CancellationException) {
                return error.toExternalCallStatus()
            }

            if (error.isUnauthorized()) {
                // we used to logout here, but when there are multiple requests in flight, the logout
                // would overrule the reauth that would come back a bit later - so just log this condition
                Timber.i("ApiErrorHandler::handleError: Unauthorized")
            } else if (!message.isNullOrBlank()) {
                Timber.i("ApiErrorHandler::handleError: $message")
                errorRepository.showError(message)
            }

            return error.toExternalCallStatus()
        }

        fun handleEmpty(
            empty: ApiResult.Empty,
            message: String?,
        ): ExternalCallStatus<Nothing> {
            if (!message.isNullOrBlank()) {
                errorRepository.showError(message)
            }

            return empty.toExternalCallStatus()
        }
    }
