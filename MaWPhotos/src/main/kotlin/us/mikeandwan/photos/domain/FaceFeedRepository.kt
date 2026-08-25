package us.mikeandwan.photos.domain

import java.net.HttpURLConnection
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.FaceFeedFilter
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.api.Media as ApiMedia

/**
 * The media one person - or anyone in a clan - appears in, accumulated a page at a time.
 *
 * Held here rather than in a view model because the grid and the pager are separate screens over
 * the same list, the same way the random feed is shared between its two. Only one feed is on screen
 * at a time, so one accumulation is enough, and pointing this at a new subject or filter starts a
 * new one.
 */
class FaceFeedRepository
    @Inject
    constructor(
        private val api: FaceApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_MEDIA = "Unable to load media at this time.  Please try again later."
        }

        private val _media = MutableStateFlow<List<Media>>(emptyList())
        val media = _media.asStateFlow()

        private val _hasMore = MutableStateFlow(false)
        val hasMore = _hasMore.asStateFlow()

        private val _subject = MutableStateFlow<FaceFeedSubject?>(null)
        val subject = _subject.asStateFlow()

        private val _filter = MutableStateFlow(FaceFeedFilter())
        val filter = _filter.asStateFlow()

        private var nextOffset = 0

        // whether anything has come back yet, which is what tells an exhausted feed apart from one
        // that has not started.  it cannot be inferred from nextOffset: a feed the API answers
        // whole in one page comes back with no more results and an offset of zero, and reading that
        // as "not started" sends the very same request again and appends the same rows twice.
        private var hasLoadedAPage = false

        // a grid can ask for the next page more than once before the first ask lands - two rows of
        // a scroll are enough - and a second request at the same offset would append the same page
        // twice
        private var isLoading = false

        // bumped every time the feed starts over.  a request already in flight when that happens
        // was asked under the old subject or filter, and its rows do not belong in the new list -
        // appending them anyway leaves the grid holding media the filter excludes, and duplicate
        // ids once the new pages arrive alongside them.
        private var generation = 0

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
        fun initialize(subject: FaceFeedSubject) {
            if (_subject.value == subject) {
                return
            }

            _subject.update { subject }
            _filter.update { FaceFeedFilter() }

            reset()
        }

        // narrowing to favorites or reshuffling changes which rows come back and in what order, so
        // the accumulated list cannot be kept
        fun setFilter(filter: FaceFeedFilter) {
            if (_filter.value == filter) {
                return
            }

            _filter.update { filter }

            reset()
        }

        fun loadNextPage() =
            flow {
                val subject = _subject.value

                if (subject == null || isLoading || (hasLoadedAPage && !_hasMore.value)) {
                    return@flow
                }

                isLoading = true
                val generationAsked = generation
                emit(ExternalCallStatus.Loading)

                try {
                    val filter = _filter.value
                    val offset = nextOffset

                    val result = fetch(subject, offset, filter)

                    // the feed started over while this was in flight, so what came back answers a
                    // question nobody is asking any more
                    if (generationAsked != generation) {
                        return@flow
                    }

                    when (result) {
                        is ApiResult.Success -> {
                            emit(handleResults(result.result))
                        }

                        // an empty feed answers 404 rather than an empty first page - see the notes
                        // on GetPersonMedia in maw-media.  a person the caller cannot see answers
                        // the same way on purpose, so both land here as simply nothing to show.
                        is ApiResult.Error -> {
                            if (result.errorCode == HttpURLConnection.HTTP_NOT_FOUND) {
                                _hasMore.update { false }

                                emit(ExternalCallStatus.Success(emptyList()))
                            } else {
                                emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_MEDIA))
                            }
                        }

                        is ApiResult.Empty -> {
                            emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_MEDIA))
                        }
                    }
                } finally {
                    // a superseded request leaves the flag to whichever request replaced it
                    if (generationAsked == generation) {
                        isLoading = false
                    }
                }
            }

        fun updateMedia(updated: Media) {
            _media.update { currentList ->
                val index = currentList.indexOfFirst { it.id == updated.id }

                if (index < 0) {
                    currentList
                } else {
                    currentList.toMutableList().also { it[index] = updated }
                }
            }
        }

        fun clear() {
            _subject.update { null }
            _filter.update { FaceFeedFilter() }

            reset()
        }

        private suspend fun fetch(
            subject: FaceFeedSubject,
            offset: Int,
            filter: FaceFeedFilter,
        ): ApiResult<SearchResults<ApiMedia>> =
            when (subject) {
                is FaceFeedSubject.Person -> {
                    api.getPersonMedia(subject.personId, offset, filter.favoritesOnly, filter.seed)
                }

                is FaceFeedSubject.Clan -> {
                    api.getClanMedia(subject.clanId, offset, filter.favoritesOnly, filter.seed)
                }
            }

        private fun handleResults(results: SearchResults<ApiMedia>): ExternalCallStatus<List<Media>> {
            val page = results.results.map { it.toDomainMedia() }

            _media.update { it + page }
            _hasMore.update { results.hasMoreResults }
            nextOffset = results.nextOffset
            hasLoadedAPage = true

            return ExternalCallStatus.Success(page)
        }

        private fun reset() {
            generation++
            isLoading = false
            hasLoadedAPage = false
            _media.update { emptyList() }
            _hasMore.update { false }
            nextOffset = 0
        }
    }
