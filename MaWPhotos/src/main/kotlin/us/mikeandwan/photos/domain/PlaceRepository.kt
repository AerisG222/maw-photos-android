package us.mikeandwan.photos.domain

import androidx.collection.LruCache
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.broadestFirst

/**
 * The tree of countries, states and cities the caller has media in.
 *
 * The API scopes every listing to what the caller can already see, so a place holding nothing
 * visible to them simply does not arrive - there is no permission to check on this side, and an
 * empty listing is a real answer rather than a refusal.
 *
 * Reads are unpaged: each one is scoped to a single parent, so the largest answer is one country's
 * states or one state's cities, and the whole tree is a few hundred nodes.
 *
 * The API also takes a name search across the whole tree and a filter by kind. Neither is used
 * here: on a phone the tree is walked rather than queried, and the drill-down is short enough -
 * three levels - that a search box would be a second way to do what tapping already does.
 */
@Singleton
class PlaceRepository
    @Inject
    constructor(
        private val api: PlaceApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_PLACES = "Unable to load places at this time.  Please try again later."
            private const val ERR_MSG_LOAD_PLACE = "Unable to load this place at this time.  Please try again later."
            private const val ERR_MSG_LOAD_MEDIA_PLACES =
                "Unable to load where this was taken at this time.  Please try again later."

            // a few screens' worth of paging in either direction, the same as the face cache - and
            // for the same reason, since both answer the item a pager happens to be sitting on
            private const val MEDIA_PLACES_CACHE_SIZE = 32
        }

        // every place that has come back, by id.  the breadcrumb above a place is drawn from here,
        // which is what makes walking back up the tree free: drilling down populated it on the way
        // through, and only a cold start into the middle of the tree pays for a read.
        private val _placesById = MutableStateFlow<Map<Uuid, Place>>(emptyMap())
        val placesById = _placesById.asStateFlow()

        // the root of the tree, held on its own so the navigation rail can offer the countries as a
        // jump without fetching anything: the browse screen is the only way into this area, and it
        // lists them on the way in.
        private val _countries = MutableStateFlow<List<Place>>(emptyList())
        val countries = _countries.asStateFlow()

        // where each media item was taken, by media id.  a pager walks back and forth over the same
        // handful of items, and where a photograph was taken does not move.
        private val cachedMediaPlaces = LruCache<Uuid, List<Place>>(MEDIA_PLACES_CACHE_SIZE)

        /** One level of the tree - the countries when [parentId] is null. */
        fun getPlaces(parentId: Uuid? = null) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getPlaces(parentId)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_PLACES))
                    }

                    // a body-less success.  an empty listing normally arrives as an empty array and
                    // lands below as a success with no rows, which at the bottom of the tree is the
                    // ordinary answer rather than a failure - this is the same thing said
                    // differently.
                    is ApiResult.Empty -> {
                        emit(ExternalCallStatus.Success(emptyList()))
                    }

                    is ApiResult.Success -> {
                        val places = result.result.map { it.toDomainPlace() }

                        cache(places)

                        if (parentId == null) {
                            _countries.update { places }
                        }

                        emit(ExternalCallStatus.Success(places))
                    }
                }
            }

        /**
         * One place, for the name and the counts the browse puts above its listing.
         *
         * A 404 is a place the caller may see nothing at, or one a merge has since folded away -
         * the API does not tell those apart on purpose, and neither does this.
         */
        fun getPlace(placeId: Uuid) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getPlace(placeId)) {
                    is ApiResult.Error -> {
                        if (result.errorCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            emit(ExternalCallStatus.Error(ERR_MSG_LOAD_PLACE))
                        } else {
                            emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_PLACE))
                        }
                    }

                    is ApiResult.Empty -> {
                        emit(ExternalCallStatus.Error(ERR_MSG_LOAD_PLACE))
                    }

                    is ApiResult.Success -> {
                        val place = result.result.toDomainPlace()

                        cache(listOf(place))

                        emit(ExternalCallStatus.Success(place))
                    }
                }
            }

        /**
         * Where one media item was taken - the city, its state and its country, broadest first.
         *
         * Not the same thing as [getAncestors]: that walks up from a place the caller is already
         * looking at, while this answers which places a media item was geocoded into at all. A
         * media item that was never placed answers with nothing, which is an ordinary answer.
         *
         * Sorted here rather than left to the caller, so the card and anything else that comes to
         * read this agree on an order the API does not promise.
         */
        fun getMediaPlaces(mediaId: Uuid) =
            flow {
                cachedMediaPlaces[mediaId]?.let {
                    emit(ExternalCallStatus.Success(it))

                    return@flow
                }

                emit(ExternalCallStatus.Loading)

                when (val result = api.getMediaPlaces(mediaId)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_MEDIA_PLACES))
                    }

                    // a media item nobody geocoded answers with an empty array, which lands below.
                    // a body-less success says the same thing differently, and caching it stops the
                    // pager asking again every time it comes back to the item.
                    is ApiResult.Empty -> {
                        cachedMediaPlaces.put(mediaId, emptyList())

                        emit(ExternalCallStatus.Success(emptyList()))
                    }

                    is ApiResult.Success -> {
                        val places = result.result
                            .map { it.toDomainPlace() }
                            .sortedWith(broadestFirst)

                        cache(places)
                        cachedMediaPlaces.put(mediaId, places)

                        emit(ExternalCallStatus.Success(places))
                    }
                }
            }

        /**
         * The chain of places above and including one, country first.
         *
         * Its own read rather than something walked up through [placesById]: a browse restored into
         * the middle of the tree holds none of the levels above it, and this always answers whole.
         */
        fun getAncestors(placeId: Uuid) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getPlaceAncestors(placeId)) {
                    is ApiResult.Error -> {
                        // the strip is a convenience over a place that is already on screen, so a
                        // failure to draw it is not worth a message of its own
                        emit(ExternalCallStatus.Error(result.error, result.exception))
                    }

                    is ApiResult.Empty -> {
                        emit(ExternalCallStatus.Success(emptyList()))
                    }

                    is ApiResult.Success -> {
                        emit(ExternalCallStatus.Success(result.result.map { it.toDomainPlaceAncestor() }))
                    }
                }
            }

        private fun cache(places: List<Place>) {
            if (places.isEmpty()) {
                return
            }

            _placesById.update { current -> current + places.associateBy { it.id } }
        }
    }
