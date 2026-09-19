package us.mikeandwan.photos.domain.services

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Place

/**
 * Where whatever is currently on screen was taken.
 *
 * Null means nobody has asked yet, or the item changed and the last answer went with it; an empty
 * list means the item was never placed, which is an ordinary answer and worth saying out loud
 * rather than drawing as a blank card.  A failed read also lands as an empty list - the call has
 * already been reported the way every other failed call is, and a second voice saying so over a
 * photograph the user is looking at would not help them.
 */
@ViewModelScoped
class MediaPlaceService
    @Inject
    constructor(
        private val placeRepository: PlaceRepository,
    ) {
        private val _places = MutableStateFlow<List<Place>?>(null)
        val places = _places.asStateFlow()

        suspend fun fetchPlaces(mediaId: Uuid) {
            // cleared first so a second item's card is empty while it loads rather than showing
            // where the previous one was taken
            _places.value = null

            _places.value = placeRepository
                .getMediaPlaces(mediaId)
                .filterIsInstance<ExternalCallStatus.Success<List<Place>>>()
                .map { it.result }
                .firstOrNull()
                ?: emptyList()
        }

        fun clear() {
            _places.value = null
        }
    }
