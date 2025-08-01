package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject

class PhotoApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _photoApi: PhotoApi by lazy { retrofit.create(PhotoApi::class.java) }

    suspend fun getRecentCategories(sinceId: Int): ApiResult<ApiCollection<Category>> {
        return makeApiCall(::getRecentCategories.name, suspend { _photoApi.getRecentCategories(/*sinceId*/) })
    }

    suspend fun getPhotos(categoryId: Int): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getPhotos.name, suspend { _photoApi.getPhotosByCategory(categoryId) })
    }

    suspend fun getRandomPhotos(count: Int): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getRandomPhotos.name, suspend { _photoApi.getRandomPhotos(count) })
    }

    suspend fun getExifData(photoId: Int): ApiResult<ExifData> {
        return makeApiCall(::getExifData.name, suspend { _photoApi.getExifData(photoId) })
    }

    suspend fun getComments(photoId: Int): ApiResult<ApiCollection<Comment>> {
        return makeApiCall(::getComments.name, suspend { _photoApi.getComments(photoId) })
    }

    suspend fun getRatings(photoId: Int): ApiResult<Rating> {
        return makeApiCall(::getRatings.name, suspend { _photoApi.getRatings(photoId) })
    }

    suspend fun setRating(photoId: Int, rating: Short): ApiResult<Rating> {
        val rp = RatePhoto(photoId, rating)

        return makeApiCall(::setRating.name, suspend { _photoApi.ratePhoto(photoId, rp) })
    }

    suspend fun addComment(photoId: Int, comment: String): ApiResult<ApiCollection<Comment>> {
        val cp = CommentPhoto(photoId, comment)

        return makeApiCall(::addComment.name, suspend { _photoApi.addCommentForPhoto(photoId, cp) })
    }
}
