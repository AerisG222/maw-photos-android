package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface MediaApi {
    @GET("media/{mediaId}/metadata")
    suspend fun getExifData(
        @Path("mediaId") mediaId: Uuid,
    ): Response<JsonElement>

    @GET("media/random/{count}")
    suspend fun getRandomMedia(
        @Path("count") count: Int,
    ): Response<List<Media>>

    @GET("media/{mediaId}/comments")
    suspend fun getComments(
        @Path("mediaId") mediaId: Uuid,
    ): Response<List<Comment>>

    // answers an empty list rather than a 404 for a media item nobody was detected in, so an empty
    // overlay and a missing one are the same thing here
    @GET("media/{mediaId}/faces")
    suspend fun getFaces(
        @Path("mediaId") mediaId: Uuid,
    ): Response<List<Face>>

    @PUT("media/{mediaId}/favorite")
    suspend fun setFavorite(
        @Path("mediaId") mediaId: Uuid,
        @Body favoriteRequest: FavoriteRequest,
    ): Response<Media>

    @POST("media/{mediaId}/comments")
    suspend fun addComment(
        @Path("mediaId") mediaId: Uuid,
        @Body commentRequest: CommentRequest,
    ): Response<Comment>
}
