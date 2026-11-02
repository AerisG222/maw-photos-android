package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedOptions
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.MediaFilters

/**
 * The media feeds in use, one per subject.
 *
 * Held here rather than in a view model because the grid and the pager are separate screens over
 * the same list of media, the same way the random feed is shared between its two.
 *
 * One per subject rather than one at a time, because more than one can be on the back stack: moving
 * from one person to another in the rail leaves the first underneath, and going back to it - or a
 * back gesture previewing it - has to find that person's own media. Each screen holds its feed from
 * [acquire] to [release], so a feed lasts exactly as long as something on a back stack can show it.
 *
 * Only ever touched from the main thread, which is where the view models holding feeds run.
 */
@Singleton
class MediaFeedRepository
    @Inject
    constructor(
        private val faceApi: FaceApiClient,
        private val placeApi: PlaceApiClient,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
        private val placePreferenceRepository: PlacePreferenceRepository,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        private class HeldFeed(
            val feed: MediaFeed,
            var holders: Int = 0,
        )

        private val feeds = mutableMapOf<MediaFeedSubject, HeldFeed>()

        // which of the two listings is being browsed.  it outlives the subject on purpose - it is
        // how somebody wants to look at a feed rather than something about one subject, so moving
        // from one person, clan or place to the next stays on the listing they were reading.
        private val _showCategories = MutableStateFlow(false)
        val showCategories = _showCategories.asStateFlow()

        /**
         * The feed for a subject, for as long as the caller holds it - pair this with [release].
         *
         * Whoever is first gets a new feed, starting from what is saved for its area with a seed of
         * its own if that is shuffled. Anyone after that gets the same one, which is what lets the
         * pager open over what the grid has already accumulated and narrowed.
         */
        suspend fun acquire(subject: MediaFeedSubject): MediaFeed {
            val held = feeds[subject] ?: newFeed(subject)

            held.holders++

            return held.feed
        }

        fun release(subject: MediaFeedSubject) {
            val held = feeds[subject] ?: return

            held.holders--

            if (held.holders <= 0) {
                feeds.remove(subject)
            }
        }

        // per area, the same way the category labels are: a person or a clan answers to the people
        // preference and a place to its own
        fun savedOptions(subject: MediaFeedSubject): Flow<MediaFeedOptions> =
            when (subject) {
                is MediaFeedSubject.Place -> placePreferenceRepository.getFeedOptions()
                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.getFeedOptions()
            }

        suspend fun saveFilters(
            subject: MediaFeedSubject,
            filters: MediaFilters,
        ) {
            when (subject) {
                is MediaFeedSubject.Place -> placePreferenceRepository.setFeedFilters(filters)

                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.setFeedFilters(
                    filters,
                )
            }
        }

        suspend fun saveShuffled(
            subject: MediaFeedSubject,
            isShuffled: Boolean,
        ) {
            when (subject) {
                is MediaFeedSubject.Place -> placePreferenceRepository.setFeedShuffled(isShuffled)

                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.setFeedShuffled(
                    isShuffled,
                )
            }
        }

        fun setShowCategories(showCategories: Boolean) {
            _showCategories.update { showCategories }
        }

        // to every feed, because the same photograph can be in several: toggling a favorite on it
        // in one person's feed is just as true of it in the feed of whoever else is in the picture
        fun updateMedia(updated: Media) {
            feeds.values.forEach { it.feed.updateMedia(updated) }
        }

        private suspend fun newFeed(subject: MediaFeedSubject): HeldFeed {
            val saved = savedOptions(subject).first()

            // asked again because reading the preferences suspends, and the grid and the pager can
            // both be on their way in at once
            return feeds.getOrPut(subject) {
                HeldFeed(
                    MediaFeed(
                        subject,
                        faceApi,
                        placeApi,
                        apiErrorHandler,
                        MediaFeedFilter().withOptions(saved),
                    ),
                )
            }
        }
    }
