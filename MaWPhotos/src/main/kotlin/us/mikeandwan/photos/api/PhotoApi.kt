package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*
import kotlin.uuid.Uuid

internal interface PhotoApi {
    @GET("categories/")
    suspend fun getRecentCategories(): Response<ApiCollection<Category>>

    @GET("photos/{photoId}/exif")
    suspend fun getExifData(@Path("photoId") photoId: Uuid): Response<ExifData>

    @GET("photos/random/{count}")
    suspend fun getRandomPhotos(@Path("count") count: Int): Response<ApiCollection<Photo>>

    @GET("photos/{photoId}/comments")
    suspend fun getComments(@Path("photoId") photoId: Uuid): Response<ApiCollection<Comment>>

    @GET("photos/{photoId}/rating")
    suspend fun getRatings(@Path("photoId") photoId: Uuid): Response<Rating>

    @GET("photo-categories/{categoryId}/photos")
    suspend fun getPhotosByCategory(@Path("categoryId") categoryId: Uuid): Response<ApiCollection<Photo>>

    @PATCH("photos/{photoId}/rating")
    suspend fun ratePhoto(@Path("photoId") photoId: Uuid, @Body rating: RatePhoto): Response<Rating>

    @POST("photos/{photoId}/comments")
    suspend fun addCommentForPhoto(
        @Path("photoId") photoId: Uuid,
        @Body commentPhoto: CommentPhoto
    ): Response<ApiCollection<Comment>>
}
