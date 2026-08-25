package us.mikeandwan.photos.domain.services

import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.MediaFaceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.DetectedFace
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.FaceHighlight

/**
 * The faces to draw over whatever is currently on screen.
 *
 * A failure leaves nothing to draw rather than saying so: the boxes are an embellishment over the
 * media, and an error banner over a photo the user is looking at would cost more than the overlay
 * is worth.  The call has already been reported the way every other failed call is.
 */
class MediaFaceService
    @Inject
    constructor(
        private val mediaFaceRepository: MediaFaceRepository,
        private val peopleRepository: PeopleRepository,
    ) {
        private val detected = MutableStateFlow<List<DetectedFace>>(emptyList())

        /**
         * The boxes to draw, labelled from the people list rather than fetched with a name attached.
         *
         * The two are joined here because they arrive separately: the boxes come from the media,
         * the names from a list the app holds whole.  Reading that list later fills the labels in
         * without asking for the faces again, and a face whose person is not in it - unassigned, or
         * somebody this caller may not know about - simply goes unlabelled.
         *
         * A plain flow rather than a state flow, so this owns no scope of its own to leak.
         */
        val faces = combine(detected, peopleRepository.people) { faces, people ->
            val namesByPersonId = people.associate { it.id to it.name }

            faces.map { face ->
                FaceHighlight(
                    id = face.id,
                    personId = face.personId,
                    name = face.personId?.let { namesByPersonId[it] },
                    boxX = face.boxX,
                    boxY = face.boxY,
                    boxWidth = face.boxWidth,
                    boxHeight = face.boxHeight,
                )
            }
        }

        suspend fun fetchFaces(mediaId: Uuid) =
            coroutineScope {
                // the labels need the people list, which the pager may well have been opened without
                // - browsing a category never touches it.  held for fifteen minutes, so this is
                // usually free, and it runs alongside rather than delaying the boxes behind it.
                launch { peopleRepository.getPeople().collect { } }

                detected.value = mediaFaceRepository
                    .getFaces(mediaId)
                    .filterIsInstance<ExternalCallStatus.Success<List<DetectedFace>>>()
                    .map { it.result }
                    .firstOrNull()
                    ?: emptyList()
            }

        fun clear() {
            detected.value = emptyList()
        }
    }
