package us.mikeandwan.photos.api

import java.io.IOException
import java.util.Locale
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Error(
        val error: String,
        val errorCode: Int? = null,
        val exception: Throwable? = null,
    ) : ApiResult<Nothing>()

    data object Empty : ApiResult<Nothing>()

    data class Success<out T>(
        val result: T,
    ) : ApiResult<T>()

    companion object {
        fun <T> build(response: Response<T>?): ApiResult<T> {
            if (response == null) {
                return Error("Response was null.  Unable to extract result from API call.")
            }

            if (response.isSuccessful) {
                val result = response.body()

                return if (result == null) {
                    Empty
                } else {
                    Success<T>(result)
                }
            } else {
                val body = response.errorBody()
                var message = response.message()

                if (body != null) {
                    message = try {
                        body.string()
                    } catch (ioe: IOException) {
                        response.message()
                    }
                }

                val error = String.format(
                    Locale.ENGLISH,
                    "api error response: %d | %s",
                    response.code(),
                    message,
                )

                return Error(error, response.code())
            }
        }
    }
}
