package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryLabels
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedSubject
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
class MediaFeedRepository
    @Inject
    constructor(
        private val faceApi: FaceApiClient,
        private val placeApi: PlaceApiClient,
        apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_MEDIA = "Unable to load media at this time.  Please try again later."
            private const val ERR_MSG_LOAD_CATEGORIES =
                "Unable to load categories at this time.  Please try again later."
        }

        private val mediaPager = MediaFeedPager<ApiMedia, Media>(
            apiErrorHandler = apiErrorHandler,
            errorMessage = ERR_MSG_LOAD_MEDIA,
            idOf = { it.id },
            toDomain = { it.toDomainMedia() },
        )

        private val categoryPager = MediaFeedPager<ApiCategory, Category>(
            apiErrorHandler = apiErrorHandler,
            errorMessage = ERR_MSG_LOAD_CATEGORIES,
            idOf = { it.id },
            toDomain = { it.toDomainCategory() },
        )

        val media = mediaPager.items
        val hasMore = mediaPager.hasMore

        val categories = categoryPager.items
        val hasMoreCategories = categoryPager.hasMore

        private val _subject = MutableStateFlow<MediaFeedSubject?>(null)
        val subject = _subject.asStateFlow()

        private val _filter = MutableStateFlow(MediaFeedFilter())
        val filter = _filter.asStateFlow()

        // which of the two listings is being browsed.  it outlives the subject on purpose - it is
        // how somebody wants to look at a feed rather than something about one subject, so moving
        // from one person, clan or place to the next stays on the listing they were reading.
        private val _showCategories = MutableStateFlow(false)
        val showCategories = _showCategories.asStateFlow()

        // what those categories say about themselves.  it outlives the subject for the same reason
        // the choice of listing does, and is deliberately not part of the filter: nothing about it
        // changes which rows come back or in what order, so nothing is refetched when it changes.
        private val _categoryLabels = MutableStateFlow(CategoryLabels())
        val categoryLabels = _categoryLabels.asStateFlow()

        /**
         * Points the feed at a subject, keeping what has already been accumulated when it is
         * already the one being shown - which is what lets the pager hand back to the grid without
         * refetching everything the user scrolled through.
         *
         * The subject alone decides that.  The filter belongs to the feed rather than to whoever
         * points at it: the grid is what narrows and reshuffles, and both screens call this on the
         * way in, so treating a filter as part of the identity would have the pager reset the feed
         * to an unfiltered first page and lose the item that was tapped.  A new subject is a new
         * feed, and starts unfiltered.
         */
        fun initialize(subject: MediaFeedSubject) {
            if (_subject.value == subject) {
                return
            }

            _subject.update { subject }
            _filter.update { MediaFeedFilter() }

            reset()
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
            if (current.favoritesOnly != filter.favoritesOnly) {
                categoryPager.reset()
            }
        }

        fun setShowCategories(showCategories: Boolean) {
            _showCategories.update { showCategories }
        }

        fun setCategoryLabels(labels: CategoryLabels) {
            _categoryLabels.update { labels }
    }

        fun loadNextPage() =
            when (val subject = _subject.value) {
                null -> emptyFlow()

                else -> mediaPager.loadNextPage { offset ->
                    val filter = _filter.value

                    when (subject) {
                        is MediaFeedSubject.Person -> {
                            faceApi.getPersonMedia(subject.personId, offset, filter.favoritesOnly, filter.seed)
                        }

                        is MediaFeedSubject.Clan -> {
                            faceApi.getClanMedia(subject.clanId, offset, filter.favoritesOnly, filter.seed)
                        }

                        is MediaFeedSubject.Place -> {
                            placeApi.getPlaceMedia(subject.placeId, offset, filter.favoritesOnly, filter.seed)
                        }
                    }
                }
            }

        fun loadNextPageOfCategories() =
            when (val subject = _subject.value) {
                null -> emptyFlow()

                else -> categoryPager.loadNextPage { offset ->
                    val favoritesOnly = _filter.value.favoritesOnly

                    when (subject) {
                        is MediaFeedSubject.Person -> {
                            faceApi.getPersonCategories(subject.personId, offset, favoritesOnly)
                        }

                        is MediaFeedSubject.Clan -> {
                            faceApi.getClanCategories(subject.clanId, offset, favoritesOnly)
                        }

                        is MediaFeedSubject.Place -> {
                            placeApi.getPlaceCategories(subject.placeId, offset, favoritesOnly)
                        }
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
