package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject
import kotlin.uuid.Uuid

class MediaApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _mediaApi: MediaApi by lazy { retrofit.create(MediaApi::class.java) }

    suspend fun getExifData(mediaId: Uuid): ApiResult<ExifData> {
        return makeApiCall(::getExifData.name, suspend { _mediaApi.getExifData(mediaId) })
    }

    suspend fun getRandomMedia(count: Int): ApiResult<List<Media>> {
        return makeApiCall(::getRandomMedia.name, suspend { _mediaApi.getRandomMedia(count) })
    }

    suspend fun getComments(mediaId: Uuid): ApiResult<List<Comment>> {
        return makeApiCall(::getComments.name, suspend { _mediaApi.getComments(mediaId) })
    }

    suspend fun setFavorite(mediaId: Uuid, isFavorite: Boolean): ApiResult<Media> {
        val req = FavoriteRequest(isFavorite)

        return makeApiCall(::setFavorite.name, suspend { _mediaApi.setFavorite(mediaId, req) })
    }

    suspend fun addComment(mediaId: Uuid, comment: String): ApiResult<Comment> {
        val req = CommentRequest(comment)

        return makeApiCall(::addComment.name, suspend { _mediaApi.addComment(mediaId, req) })
    }
}
