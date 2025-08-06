package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*
import kotlin.uuid.Uuid

internal interface MediaApi {
    @GET("media/{mediaId}/exif")
    suspend fun getExifData(@Path("mediaId") mediaId: Uuid): Response<ExifData>

    @GET("media/random/{count}")
    suspend fun getRandomMedia(@Path("count") count: Int): Response<List<Media>>

    @GET("media/{mediaId}/comments")
    suspend fun getComments(@Path("mediaId") mediaId: Uuid): Response<List<Comment>>

    @POST("media/{mediaId}/favorite")
    suspend fun setFavorite(@Path("mediaId") mediaId: Uuid, @Body favoriteRequest: FavoriteRequest): Response<Media>

    @POST("media/{mediaId}/comments")
    suspend fun addComment(
        @Path("mediaId") mediaId: Uuid,
        @Body commentRequest: CommentRequest
    ): Response<Comment>
}
