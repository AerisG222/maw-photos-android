package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*

internal interface PhotoApi {
//    @GET("photo-categories/recent/{sinceId}")
//    suspend fun getRecentCategories(@Path("sinceId") sinceId: Int): Response<ApiCollection<Category>>

    @GET("categories/")
    suspend fun getRecentCategories(): Response<ApiCollection<Category>>

    @GET("photos/{photoId}/exif")
    suspend fun getExifData(@Path("photoId") photoId: Int): Response<ExifData>

    @GET("photos/random/{count}")
    suspend fun getRandomPhotos(@Path("count") count: Int): Response<ApiCollection<Photo>>

    @GET("photos/{photoId}/comments")
    suspend fun getComments(@Path("photoId") photoId: Int): Response<ApiCollection<Comment>>

    @GET("photos/{photoId}/rating")
    suspend fun getRatings(@Path("photoId") photoId: Int): Response<Rating>

    @GET("photo-categories/{categoryId}/photos")
    suspend fun getPhotosByCategory(@Path("categoryId") categoryId: Int): Response<ApiCollection<Photo>>

    @PATCH("photos/{photoId}/rating")
    suspend fun ratePhoto(@Path("photoId") photoId: Int, @Body rating: RatePhoto): Response<Rating>

    @POST("photos/{photoId}/comments")
    suspend fun addCommentForPhoto(
        @Path("photoId") photoId: Int,
        @Body commentPhoto: CommentPhoto
    ): Response<ApiCollection<Comment>>
}
