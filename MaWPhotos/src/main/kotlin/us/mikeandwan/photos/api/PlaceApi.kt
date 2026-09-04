package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// browsing by where a photograph was taken.  everything here is gated by the API on plain media
// reading, so unlike the face routes there is no scope to check before offering it.
internal interface PlaceApi {
    // deliberately unpaged by the API - each request is scoped to one parent, so the largest answer
    // is one country's states or one state's cities.  a null parent is omitted by retrofit, which
    // is what the API reads as the root.
    //
    // it also takes a kind filter and a name search across the whole tree; neither is declared
    // here, because this client walks the tree rather than querying it - see PlaceRepository.
    @GET("places")
    suspend fun getPlaces(
        @Query("parent") parentId: Uuid?,
    ): Response<List<Place>>

    @GET("places/{placeId}")
    suspend fun getPlace(
        @Path("placeId") placeId: Uuid,
    ): Response<Place>

    @GET("places/{placeId}/ancestors")
    suspend fun getPlaceAncestors(
        @Path("placeId") placeId: Uuid,
    ): Response<List<PlaceAncestor>>

    // everything at a place and beneath it, so a country answers with the photographs from every
    // one of its states and cities
    @GET("places/{placeId}/media")
    suspend fun getPlaceMedia(
        @Path("placeId") placeId: Uuid,
        @Query("o") offset: Int,
        @Query("f") favoritesOnly: Boolean?,
        @Query("seed") seed: Long?,
    ): Response<SearchResults<Media>>

    // the same media, rolled up to the categories holding it - no seed, because a shuffled list of
    // categories would mean nothing
    @GET("places/{placeId}/categories")
    suspend fun getPlaceCategories(
        @Path("placeId") placeId: Uuid,
        @Query("o") offset: Int,
        @Query("f") favoritesOnly: Boolean?,
    ): Response<SearchResults<Category>>
}
