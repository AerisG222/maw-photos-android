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

        // a grid can ask for the next page more than once before the first ask lands - two rows of
        // a scroll are enough - and a second request at the same offset would append the same page
        // twice
        private var isLoading = false

        /**
         * Points the feed at a subject, keeping what has already been accumulated when it is
         * already the one being shown - which is what lets the pager hand back to the grid without
         * refetching everything the user scrolled through.
         */
        fun initialize(
            subject: FaceFeedSubject,
            filter: FaceFeedFilter = FaceFeedFilter(),
        ) {
            if (_subject.value == subject && _filter.value == filter) {
                return
            }

            _subject.update { subject }
            _filter.update { filter }

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

                if (subject == null || isLoading || (nextOffset > 0 && !_hasMore.value)) {
                    return@flow
                }

                isLoading = true
                emit(ExternalCallStatus.Loading)

                try {
                    val filter = _filter.value
                    val offset = nextOffset

                    when (val result = fetch(subject, offset, filter)) {
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
                    isLoading = false
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

            return ExternalCallStatus.Success(page)
        }

        private fun reset() {
            _media.update { emptyList() }
            _hasMore.update { false }
            nextOffset = 0
        }
    }
