package us.mikeandwan.photos.api

import javax.inject.Inject
import kotlin.uuid.Uuid
import retrofit2.Retrofit

class FaceApiClient
    @Inject
    constructor(
        retrofit: Retrofit,
    ) : BaseApiClient() {
        private val _faceApi: FaceApi by lazy { retrofit.create(FaceApi::class.java) }

        suspend fun getPeople(): ApiResult<List<Person>> =
            makeApiCall(
                ::getPeople.name,
                suspend {
                    _faceApi.getPeople()
                },
            )

        suspend fun getPersonMedia(
            personId: Uuid,
            offset: Int,
            favoritesOnly: Boolean,
            seed: Long?,
        ): ApiResult<SearchResults<Media>> =
            makeApiCall(
                ::getPersonMedia.name,
                suspend {
                    _faceApi.getPersonMedia(personId, offset, favoritesOnly.takeIf { it }, seed)
                },
            )

        suspend fun getPersonCategories(
            personId: Uuid,
            offset: Int,
            favoritesOnly: Boolean,
        ): ApiResult<SearchResults<Category>> =
            makeApiCall(
                ::getPersonCategories.name,
                suspend {
                    _faceApi.getPersonCategories(personId, offset, favoritesOnly.takeIf { it })
            },
        )

        suspend fun setPersonFavorite(
            personId: Uuid,
            isFavorite: Boolean,
        ): ApiResult<Person> {
            val req = FavoriteRequest(isFavorite)

            return makeApiCall(
                ::setPersonFavorite.name,
                suspend {
                    _faceApi.setPersonFavorite(personId, req)
                },
            )
        }

        suspend fun getClans(): ApiResult<List<Clan>> =
            makeApiCall(
                ::getClans.name,
                suspend {
                    _faceApi.getClans()
                },
            )

        suspend fun createClan(
            name: String,
            personIds: List<Uuid>,
        ): ApiResult<Clan> {
            val req = ClanRequest(name, personIds)

            return makeApiCall(
                ::createClan.name,
                suspend {
                    _faceApi.createClan(req)
                },
            )
        }

        // membership is left out entirely rather than sent unchanged, so a rename cannot race a
        // membership edit made from somewhere else
        suspend fun renameClan(
            clanId: Uuid,
            name: String,
        ): ApiResult<Clan> {
            val req = ClanRequest(name)

            return makeApiCall(
                ::renameClan.name,
                suspend {
                    _faceApi.updateClan(clanId, req)
                },
            )
        }

        suspend fun setClanPeople(
            clanId: Uuid,
            personIds: List<Uuid>,
        ): ApiResult<Clan> {
            val req = ClanPersonsRequest(personIds)

            return makeApiCall(
                ::setClanPeople.name,
                suspend {
                    _faceApi.setClanPeople(clanId, req)
                },
            )
        }

        // answers 204, which arrives here as ApiResult.Empty rather than Success - see
        // ClanRepository, which is where that is read as the success it is
        suspend fun deleteClan(clanId: Uuid): ApiResult<Unit> =
            makeApiCall(
                ::deleteClan.name,
                suspend {
                    _faceApi.deleteClan(clanId)
                },
            )

        suspend fun getClanMedia(
            clanId: Uuid,
            offset: Int,
            favoritesOnly: Boolean,
            seed: Long?,
        ): ApiResult<SearchResults<Media>> =
            makeApiCall(
                ::getClanMedia.name,
                suspend {
                    _faceApi.getClanMedia(clanId, offset, favoritesOnly.takeIf { it }, seed)
                },
            )

    suspend fun getClanCategories(
        clanId: Uuid,
        offset: Int,
        favoritesOnly: Boolean,
    ): ApiResult<SearchResults<Category>> =
        makeApiCall(
            ::getClanCategories.name,
            suspend {
                _faceApi.getClanCategories(clanId, offset, favoritesOnly.takeIf { it })
            },
        )
    }
