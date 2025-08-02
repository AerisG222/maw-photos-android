package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject

class CategoryApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _categoryApi: CategoryApi by lazy { retrofit.create(CategoryApi::class.java) }

    suspend fun getYears(): ApiResult<List<Int>> {
        return makeApiCall(::getYears.name, suspend { _categoryApi.getYears() })
    }

    suspend fun getCategoriesForYear(year: Int): ApiResult<List<Category>> {
        return makeApiCall(::getCategoriesForYear.name, suspend { _categoryApi.getCategoriesForYear(year) })
    }

    /*
    suspend fun getPhotos(categoryId: Int): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getPhotos.name, suspend { _categoryApi.getPhotosByCategory(categoryId) })
    }

    suspend fun getRandomPhotos(count: Int): ApiResult<ApiCollection<Photo>> {
        return makeApiCall(::getRandomPhotos.name, suspend { _categoryApi.getRandomPhotos(count) })
    }

    suspend fun getExifData(photoId: Int): ApiResult<ExifData> {
        return makeApiCall(::getExifData.name, suspend { _categoryApi.getExifData(photoId) })
    }

    suspend fun getComments(photoId: Int): ApiResult<ApiCollection<Comment>> {
        return makeApiCall(::getComments.name, suspend { _categoryApi.getComments(photoId) })
    }

    suspend fun getRatings(photoId: Int): ApiResult<Rating> {
        return makeApiCall(::getRatings.name, suspend { _categoryApi.getRatings(photoId) })
    }

    suspend fun setRating(photoId: Int, rating: Short): ApiResult<Rating> {
        val rp = RatePhoto(photoId, rating)

        return makeApiCall(::setRating.name, suspend { _categoryApi.ratePhoto(photoId, rp) })
    }

    suspend fun addComment(photoId: Int, comment: String): ApiResult<ApiCollection<Comment>> {
        val cp = CommentPhoto(photoId, comment)

        return makeApiCall(::addComment.name, suspend { _categoryApi.addCommentForPhoto(photoId, cp) })
    }
    */
}
