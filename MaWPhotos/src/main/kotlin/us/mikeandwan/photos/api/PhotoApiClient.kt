package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject
import kotlin.uuid.Uuid

class PhotoApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _photoApi: PhotoApi by lazy { retrofit.create(PhotoApi::class.java) }

    suspend fun getRecentCategories(sinceId: Uuid): ApiResult<ApiCollection<Category>> {
        return makeApiCall(::getRecentCategories.name, suspend { _photoApi.getRecentCategories(/*sinceId*/) })
    }

    suspend fun getPhotos(categoryId: Uuid): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getPhotos.name, suspend { _photoApi.getPhotosByCategory(categoryId) })
    }

    suspend fun getRandomPhotos(count: Int): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getRandomPhotos.name, suspend { _photoApi.getRandomPhotos(count) })
    }

    suspend fun getExifData(photoId: Uuid): ApiResult<ExifData> {
        return makeApiCall(::getExifData.name, suspend { _photoApi.getExifData(photoId) })
    }

    suspend fun getComments(photoId: Uuid): ApiResult<ApiCollection<Comment>> {
        return makeApiCall(::getComments.name, suspend { _photoApi.getComments(photoId) })
    }

    suspend fun getRatings(photoId: Uuid): ApiResult<Rating> {
        return makeApiCall(::getRatings.name, suspend { _photoApi.getRatings(photoId) })
    }

    suspend fun setRating(photoId: Uuid, rating: Short): ApiResult<Rating> {
        val rp = RatePhoto(photoId, rating)

        return makeApiCall(::setRating.name, suspend { _photoApi.ratePhoto(photoId, rp) })
    }

    suspend fun addComment(photoId: Uuid, comment: String): ApiResult<ApiCollection<Comment>> {
        val cp = CommentPhoto(photoId, comment)

        return makeApiCall(::addComment.name, suspend { _photoApi.addCommentForPhoto(photoId, cp) })
    }
}
