package us.mikeandwan.photos.api

import android.webkit.MimeTypeMap
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import timber.log.Timber
import us.mikeandwan.photos.domain.ApiErrorHandler
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import java.io.File

abstract class BaseApiClient {
    private val mimeMap by lazy { MimeTypeMap.getSingleton() }

    fun <TApiResult, TEmit> loadData(
        apiCall: suspend () -> ApiResult<TApiResult>,
        onSuccess: suspend (TApiResult) -> TEmit,
        errorMessage: String,
        apiErrorHandler: ApiErrorHandler
    ) = flow<ExternalCallStatus<TEmit>> {
        emit(ExternalCallStatus.Loading)

        when(val result = apiCall()) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, errorMessage))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, errorMessage))
            is ApiResult.Success -> {
                val res = onSuccess(result.result)
                emit(ExternalCallStatus.Success(res))
            }
        }
    }

    protected suspend fun <T> makeApiCall(name: String, apiCall: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        try {
            val response = apiCall()
            val result = ApiResult.build(response)

            when (result) {
                is ApiResult.Success -> Timber.d("$name succeeded")
                is ApiResult.Empty -> Timber.d("$name was empty")
                is ApiResult.Error -> Timber.w("$name: ${result.error}")
            }

            return result
        } catch (t: Throwable) {
            return ApiResult.Error("$name failed", null, t)
        }
    }

    protected fun getMediaTypeForFile(file: File): MediaType {
        return mimeMap
            .getMimeTypeFromExtension(file.extension)
                ?.toMediaTypeOrNull() ?: "binary/octet-stream".toMediaType()
    }
}
