package us.mikeandwan.photos.domain

import androidx.collection.LruCache
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.flow
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.DetectedFace
import us.mikeandwan.photos.domain.models.ExternalCallStatus

/**
 * The faces detected in a media item, for drawing an overlay over it.
 *
 * Nothing here is fetched unless the caller asks, and the caller only asks while face highlighting
 * is switched on - so the overlay costs exactly nothing for anyone who never turns it on.
 *
 * Cached by media id because a pager walks back and forth over the same handful of items, and a
 * published face never moves: the boxes for a given media item are as fixed as the pixels they sit
 * on, until the pipeline republishes them.
 */
class MediaFaceRepository
    @Inject
    constructor(
        private val api: MediaApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_FACES = "Unable to load faces at this time.  Please try again later."

            // a few screens' worth of paging in either direction
            private const val CACHE_SIZE = 32
        }

        private val cachedFaces = LruCache<Uuid, List<DetectedFace>>(CACHE_SIZE)

        fun getFaces(mediaId: Uuid) =
            flow {
                cachedFaces[mediaId]?.let {
                    emit(ExternalCallStatus.Success(it))

                    return@flow
                }

                emit(ExternalCallStatus.Loading)

                when (val result = api.getFaces(mediaId)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_FACES))
                    }

                    // the API answers an empty list rather than a 404 for a media item nobody was
                    // detected in, so an empty body is a shape this should never see - but a media
                    // item with no faces is an ordinary answer either way, and caching it stops the
                    // pager asking again every time it comes back
                    is ApiResult.Empty -> {
                        cachedFaces.put(mediaId, emptyList())

                        emit(ExternalCallStatus.Success(emptyList()))
                    }

                    is ApiResult.Success -> {
                        val faces = result.result.map { it.toDomainDetectedFace() }

                        cachedFaces.put(mediaId, faces)

                        emit(ExternalCallStatus.Success(faces))
                    }
                }
            }

        fun clear() {
            cachedFaces.evictAll()
        }
    }
