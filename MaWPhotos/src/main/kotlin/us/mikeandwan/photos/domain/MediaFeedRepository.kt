package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedOptions
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.toApiFilter
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Media as ApiMedia

/**
 * What one person appears in, what anyone in a clan appears in, or what was taken at one place -
 * accumulated a page at a time.
 *
 * Two listings over the one subject: the media itself, and the categories that media sits in. Both
 * are held so that switching between them keeps what has already been scrolled through, and both
 * are started over when the feed is pointed somewhere new.
 *
 * Held here rather than in a view model because the grid and the pager are separate screens over
 * the same list of media, the same way the random feed is shared between its two. Only one feed is
 * on screen at a time, so one accumulation is enough.
 */
@Singleton
class MediaFeedRepository
    @Inject
    constructor(
        private val faceApi: FaceApiClient,
        private val placeApi: PlaceApiClient,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
        private val placePreferenceRepository: PlacePreferenceRepository,
        apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_MEDIA = "Unable to load media at this time.  Please try again later."
            private const val ERR_MSG_LOAD_CATEGORIES =
                "Unable to load categories at this time.  Please try again later."
        }

        private val mediaPager = ListingPager<ApiMedia, Media>(
            apiErrorHandler = apiErrorHandler,
            errorMessage = ERR_MSG_LOAD_MEDIA,
            idOf = { it.id },
            toDomain = { it.toDomainMedia() },
        )

        private val categoryPager = ListingPager<ApiCategory, Category>(
            apiErrorHandler = apiErrorHandler,
            errorMessage = ERR_MSG_LOAD_CATEGORIES,
            idOf = { it.id },
            toDomain = { it.toDomainCategory() },
        )

        val media = mediaPager.items
        val hasMore = mediaPager.hasMore
        val isLoadingMedia = mediaPager.isLoading
        val mediaLoadFailed = mediaPager.loadFailed

        val categories = categoryPager.items
        val hasMoreCategories = categoryPager.hasMore
        val categoriesLoadFailed = categoryPager.loadFailed

        private val _subject = MutableStateFlow<MediaFeedSubject?>(null)
        val subject = _subject.asStateFlow()

        private val _filter = MutableStateFlow(MediaFeedFilter())
        val filter = _filter.asStateFlow()

        // which of the two listings is being browsed.  it outlives the subject on purpose - it is
        // how somebody wants to look at a feed rather than something about one subject, so moving
        // from one person, clan or place to the next stays on the listing they were reading.
        private val _showCategories = MutableStateFlow(false)
        val showCategories = _showCategories.asStateFlow()

        /**
         * Points the feed at a subject, keeping what has already been accumulated when it is
         * already the one being shown - which is what lets the pager hand back to the grid without
         * refetching everything the user scrolled through.
         *
         * The subject alone decides that.  The filter belongs to the feed rather than to whoever
         * points at it: the grid is what narrows and reshuffles, and both screens call this on the
         * way in, so treating a filter as part of the identity would have the pager reset the feed
         * to an unfiltered first page and lose the item that was tapped.  A new subject is a new
         * feed: it starts from what is saved for its area, with a seed of its own if that is shuffled.
         */
        suspend fun initialize(subject: MediaFeedSubject) {
            if (_subject.value == subject) {
                return
            }

            val saved = savedOptions(subject).first()

            _subject.update { subject }
            _filter.update { MediaFeedFilter().withOptions(saved) }

            reset()
        }

        // per area, the same way the category labels are: a person or a clan answers to the people
        // preference and a place to its own
        fun savedOptions(subject: MediaFeedSubject): Flow<MediaFeedOptions> =
            when (subject) {
                is MediaFeedSubject.Place -> placePreferenceRepository.getFeedOptions()
                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.getFeedOptions()
            }

        suspend fun saveFilters(filters: MediaFilters) {
            when (_subject.value) {
                null -> Unit

                is MediaFeedSubject.Place -> placePreferenceRepository.setFeedFilters(filters)

                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.setFeedFilters(
                    filters,
                )
            }
        }

        suspend fun saveShuffled(isShuffled: Boolean) {
            when (_subject.value) {
                null -> Unit

                is MediaFeedSubject.Place -> placePreferenceRepository.setFeedShuffled(isShuffled)

                is MediaFeedSubject.Person, is MediaFeedSubject.Clan -> peoplePreferenceRepository.setFeedShuffled(
                    isShuffled,
                )
            }
        }

        // narrowing to favorites or reshuffling changes which rows come back and in what order, so
        // the accumulated list cannot be kept
        fun setFilter(filter: MediaFeedFilter) {
            val current = _filter.value

            if (current == filter) {
                return
            }

            _filter.update { filter }

            mediaPager.reset()

            // the seed only orders media - there is no shuffling a list of categories, and the API
            // takes no seed for one - so the categories are only started over when the narrowing
            // itself changed
            if (current.mediaFilters != filter.mediaFilters) {
                categoryPager.reset()
            }
        }

        fun setShowCategories(showCategories: Boolean) {
            _showCategories.update { showCategories }
        }

        fun loadNextPage() =
            when (val subject = _subject.value) {
                null -> emptyFlow()
                else -> mediaPager.loadNextPage { offset -> fetchMedia(subject, offset) }
            }

        fun retryLoadNextPage() =
            when (val subject = _subject.value) {
                null -> emptyFlow()
                else -> mediaPager.retry { offset -> fetchMedia(subject, offset) }
            }

        fun loadNextPageOfCategories() =
            when (val subject = _subject.value) {
                null -> emptyFlow()
                else -> categoryPager.loadNextPage { offset -> fetchCategories(subject, offset) }
            }

        fun retryLoadNextPageOfCategories() =
            when (val subject = _subject.value) {
                null -> emptyFlow()
                else -> categoryPager.retry { offset -> fetchCategories(subject, offset) }
            }

        private suspend fun fetchMedia(
            subject: MediaFeedSubject,
            offset: Int,
        ): ApiResult<SearchResults<ApiMedia>> {
            val filter = _filter.value
            val mediaType = filter.mediaType?.toApiFilter()

            return when (subject) {
                is MediaFeedSubject.Person -> {
                    faceApi.getPersonMedia(subject.personId, offset, filter.favoritesOnly, filter.seed, mediaType)
                }

                is MediaFeedSubject.Clan -> {
                    faceApi.getClanMedia(subject.clanId, offset, filter.favoritesOnly, filter.seed, mediaType)
                }

                is MediaFeedSubject.Place -> {
                    placeApi.getPlaceMedia(subject.placeId, offset, filter.favoritesOnly, filter.seed, mediaType)
                }
            }
        }

        private suspend fun fetchCategories(
            subject: MediaFeedSubject,
            offset: Int,
        ): ApiResult<SearchResults<ApiCategory>> {
            val favoritesOnly = _filter.value.favoritesOnly
            val mediaType = _filter.value.mediaType?.toApiFilter()

            return when (subject) {
                is MediaFeedSubject.Person -> {
                    faceApi.getPersonCategories(subject.personId, offset, favoritesOnly, mediaType)
                }

                is MediaFeedSubject.Clan -> {
                    faceApi.getClanCategories(subject.clanId, offset, favoritesOnly, mediaType)
                }

                is MediaFeedSubject.Place -> {
                    placeApi.getPlaceCategories(subject.placeId, offset, favoritesOnly, mediaType)
                }
            }
        }

        fun updateMedia(updated: Media) {
            mediaPager.update(updated)
        }

        fun updateCategory(updated: Category) {
            categoryPager.update(updated)
        }

        fun clear() {
            _subject.update { null }
            _filter.update { MediaFeedFilter() }

            reset()
        }

        private fun reset() {
            mediaPager.reset()
            categoryPager.reset()
        }
    }
