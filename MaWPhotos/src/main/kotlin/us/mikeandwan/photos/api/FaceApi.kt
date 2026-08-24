package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// the API calls the resource "persons"; everything the user can see says people, so the naming
// splits at this boundary and nowhere else
internal interface FaceApi {
    // deliberately unpaged by the API - the set is a few hundred at most, and filtering it locally
    // beats a round trip per keystroke
    @GET("persons")
    suspend fun getPeople(): Response<List<Person>>

    // null query values are omitted by retrofit, which keeps an unfiltered request on the url the
    // API documents for one
    @GET("persons/{personId}/media")
    suspend fun getPersonMedia(
        @Path("personId") personId: Uuid,
        @Query("o") offset: Int,
        @Query("f") favoritesOnly: Boolean?,
        @Query("seed") seed: Long?,
    ): Response<SearchResults<Media>>

    @PUT("persons/{personId}/favorite")
    suspend fun setPersonFavorite(
        @Path("personId") personId: Uuid,
        @Body favoriteRequest: FavoriteRequest,
    ): Response<Person>

    @GET("clans")
    suspend fun getClans(): Response<List<Clan>>

    @POST("clans")
    suspend fun createClan(
        @Body clanRequest: ClanRequest,
    ): Response<Clan>

    @PUT("clans/{clanId}")
    suspend fun updateClan(
        @Path("clanId") clanId: Uuid,
        @Body clanRequest: ClanRequest,
    ): Response<Clan>

    @PUT("clans/{clanId}/persons")
    suspend fun setClanPeople(
        @Path("clanId") clanId: Uuid,
        @Body personsRequest: ClanPersonsRequest,
    ): Response<Clan>

    @DELETE("clans/{clanId}")
    suspend fun deleteClan(
        @Path("clanId") clanId: Uuid,
    ): Response<Unit>

    @GET("clans/{clanId}/media")
    suspend fun getClanMedia(
        @Path("clanId") clanId: Uuid,
        @Query("o") offset: Int,
        @Query("f") favoritesOnly: Boolean?,
        @Query("seed") seed: Long?,
    ): Response<SearchResults<Media>>
}
