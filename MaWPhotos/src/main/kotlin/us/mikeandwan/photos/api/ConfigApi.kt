package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*

internal interface ConfigApi {
    @GET("config/scales")
    suspend fun getScales(): Response<List<Scale>>
}
