package us.mikeandwan.photos.api

import retrofit2.Response
import retrofit2.http.*
import kotlin.uuid.Uuid

internal interface CategoryApi {
    @GET("categories/years")
    suspend fun getYears(): Response<List<Int>>

    @GET("categories/years/{year}")
    suspend fun getCategoriesForYear(@Path("year") year: Int): Response<List<Category>>

    @GET("categories/{categoryId}")
    suspend fun getCategory(@Path("categoryId") categoryId: Uuid): Response<Category?>

    @GET("categories/{categoryId}/media")
    suspend fun getMediaForCategory(@Path("categoryId") categoryId: Uuid): Response<List<Media>>

    @GET("categories/search")
    suspend fun search(@Query("s") query: String, @Query("o") start: Int): Response<SearchResults<Category>>
}
