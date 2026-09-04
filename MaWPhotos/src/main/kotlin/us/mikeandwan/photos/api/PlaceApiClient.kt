package us.mikeandwan.photos.api

import javax.inject.Inject
import kotlin.uuid.Uuid
import retrofit2.Retrofit

class PlaceApiClient
    @Inject
    constructor(
        retrofit: Retrofit,
    ) : BaseApiClient() {
        private val _placeApi: PlaceApi by lazy { retrofit.create(PlaceApi::class.java) }

        suspend fun getPlaces(parentId: Uuid?): ApiResult<List<Place>> =
            makeApiCall(
                ::getPlaces.name,
                suspend {
                    _placeApi.getPlaces(parentId)
                },
            )

        suspend fun getPlace(placeId: Uuid): ApiResult<Place> =
            makeApiCall(
                ::getPlace.name,
                suspend {
                    _placeApi.getPlace(placeId)
                },
            )

        suspend fun getPlaceAncestors(placeId: Uuid): ApiResult<List<PlaceAncestor>> =
            makeApiCall(
                ::getPlaceAncestors.name,
                suspend {
                    _placeApi.getPlaceAncestors(placeId)
                },
            )

        suspend fun getPlaceMedia(
            placeId: Uuid,
            offset: Int,
            favoritesOnly: Boolean,
            seed: Long?,
        ): ApiResult<SearchResults<Media>> =
            makeApiCall(
                ::getPlaceMedia.name,
                suspend {
                    _placeApi.getPlaceMedia(placeId, offset, favoritesOnly.takeIf { it }, seed)
                },
            )

        suspend fun getPlaceCategories(
            placeId: Uuid,
            offset: Int,
            favoritesOnly: Boolean,
        ): ApiResult<SearchResults<Category>> =
            makeApiCall(
                ::getPlaceCategories.name,
                suspend {
                    _placeApi.getPlaceCategories(placeId, offset, favoritesOnly.takeIf { it })
                },
            )
    }
