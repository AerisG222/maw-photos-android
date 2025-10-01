package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*

internal interface ConfigApi {
    @GET("auth/account-status")
    suspend fun getAccountStatus(): Response<AccountStatus>

    @GET("config/scales")
    suspend fun getScales(): Response<List<Scale>>
}
