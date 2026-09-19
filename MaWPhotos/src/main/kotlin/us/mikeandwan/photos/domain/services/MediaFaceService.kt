package us.mikeandwan.photos.domain.services

import dagger.hilt.android.scopes.ViewModelScoped
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
import us.mikeandwan.photos.domain.models.MediaFaces

/**
 * What the recognition pipeline found in whatever is currently on screen.
 *
 * A failure leaves nothing to show rather than saying so: the boxes are an embellishment over the
 * media, and an error banner over a photo the user is looking at would cost more than the overlay
 * is worth.  The call has already been reported the way every other failed call is.
 */
@ViewModelScoped
class MediaFaceService
    @Inject
    constructor(
        private val mediaFaceRepository: MediaFaceRepository,
        private val peopleRepository: PeopleRepository,
    ) {
        // null until somebody has asked, and again the moment the item changes - which is what
        // lets the Who card wait quietly rather than saying nobody is here while it is still asking
        private val detected = MutableStateFlow<List<DetectedFace>?>(null)

        /**
         * The boxes to draw and the people they belong to, resolved against the people list rather
         * than fetched with a name attached.
         *
         * The two are joined here because they arrive separately: the boxes come from the media,
         * the names from a list the app holds whole.  Reading that list later fills the labels in
         * without asking for the faces again, and a face whose person is not in it - unassigned, or
         * somebody this caller may not know about - simply goes unlabelled and is counted instead.
         *
         * The overlay and the details sheet's Who card both read this one value, so the two cannot
         * end up disagreeing about who is in the photograph.
         *
         * A plain flow rather than a state flow, so this owns no scope of its own to leak.
         */
        val faces = combine(detected, peopleRepository.people) { detectedFaces, people ->
            val faces = detectedFaces ?: return@combine null
            val peopleById = people.associateBy { it.id }

            val highlights = faces.map { face ->
                FaceHighlight(
                    id = face.id,
                    personId = face.personId,
                    name = face.personId?.let { peopleById[it]?.name },
                    boxX = face.boxX,
                    boxY = face.boxY,
                    boxWidth = face.boxWidth,
                    boxHeight = face.boxHeight,
                )
            }

            // a person appears once however many of their faces were detected, which is why this
            // counts faces rather than people for the leftovers
            val named = faces.mapNotNull { face -> face.personId?.let { peopleById[it] } }

            MediaFaces(
                highlights = highlights,
                people = named.distinctBy { it.id }.sortedBy { it.name },
                unnamedCount = faces.size - named.size,
            )
        }

        suspend fun fetchFaces(mediaId: Uuid) =
            coroutineScope {
                // cleared first so the card over a second photograph is blank while it loads rather
                // than naming whoever was in the one before it
                detected.value = null

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
            detected.value = null
        }
    }
