package us.mikeandwan.photos.domain

import kotlinx.coroutines.flow.flow
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import javax.inject.Inject
import kotlin.uuid.Uuid

class MediaRepository @Inject constructor (
    private val api: MediaApiClient,
    private val apiErrorHandler: ApiErrorHandler
) {
    companion object {
        private const val ERR_MSG_LOAD_EXIF = "Unable to load EXIF data at this time.  Please try again later."
        private const val ERR_MSG_LOAD_COMMENTS = "Unable to load comments at this time.  Please try again later."
        private const val ERR_MSG_ADD_COMMENTS = "Unable to add comments at this time.  Please try again later."
        private const val ERR_MSG_SET_RATING = "Unable to add ratings at this time.  Please try again later."
    }

    fun getExifData(mediaId: Uuid) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.getExifData(mediaId)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_EXIF))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_EXIF))
            is ApiResult.Success -> emit(ExternalCallStatus.Success(result.result.toDomainExifData()))
        }
    }

    fun getComments(mediaId: Uuid) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.getComments(mediaId)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_COMMENTS))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_COMMENTS))
            is ApiResult.Success -> emit(ExternalCallStatus.Success(result.result.map { it.toDomainComment() }))
        }
    }

    fun addComment(mediaId: Uuid, comment: String) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.addComment(mediaId, comment)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_ADD_COMMENTS))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_ADD_COMMENTS))
            is ApiResult.Success -> emit(ExternalCallStatus.Success(result.result.toDomainComment()))
        }
    }

    fun setFavorite(mediaId: Uuid, isFavorite: Boolean) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.setFavorite(mediaId, isFavorite)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_SET_RATING))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_SET_RATING))
            is ApiResult.Success -> emit(ExternalCallStatus.Success(result.result))
        }
    }
}
