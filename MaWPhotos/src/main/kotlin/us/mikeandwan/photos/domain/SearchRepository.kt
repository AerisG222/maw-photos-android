package us.mikeandwan.photos.domain

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import timber.log.Timber
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.database.SearchHistory
import us.mikeandwan.photos.database.SearchHistoryDao
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.SearchRequest
import us.mikeandwan.photos.domain.models.SearchSource
import us.mikeandwan.photos.domain.models.toApiFilter
import us.mikeandwan.photos.api.Category as ApiCategory

@Singleton
class SearchRepository
    @Inject
    constructor(
        private val api: CategoryApiClient,
        private val searchHistoryDao: SearchHistoryDao,
        private val searchPreferenceRepository: SearchPreferenceRepository,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_SEARCH = "Unable to search at this time.  Please try again later."
        }

        private val _searchRequest = MutableStateFlow(SearchRequest("", SearchSource.None))
        private val searchRequest = _searchRequest.asStateFlow()

        private val pager = ListingPager<ApiCategory, Category>(
            apiErrorHandler = apiErrorHandler,
            errorMessage = ERR_MSG_SEARCH,
            idOf = { it.id },
            toDomain = { it.toDomainCategory() },
        )

        val searchResults = pager.items
        val hasMoreResults = pager.hasMore
        val loadFailed = pager.loadFailed
        val isLoading = pager.isLoading

        private val _filters = MutableStateFlow(MediaFilters())
        val filters = _filters.asStateFlow()

        private val _activeSearchTerm = MutableStateFlow("")
        val activeSearchTerm = _activeSearchTerm.asStateFlow()

        fun updateCategory(updated: Category) {
            pager.update(updated)
        }

        fun getSearchHistory() =
            searchHistoryDao
                .getSearchTerms()
                .map { history -> history.map { it.toDomainSearchHistory() } }

        suspend fun clearHistory() {
            searchHistoryDao.clearHistory()
        }

        // the request goes too, not just what is on screen - otherwise searching the same term
        // again would look like a repeat of the current query and be skipped.  the filters stay:
        // they are saved, and belong to searching rather than to this one search.
        fun clearSearch() {
            _searchRequest.update { SearchRequest("", SearchSource.None) }
            _activeSearchTerm.update { "" }
            pager.reset()
        }

        // the results come a page at a time, so narrowing them is the API's job: what has been
        // accumulated cannot be kept, and whichever search is running starts over.  this applies
        // filters rather than saving them, so it runs when the saved ones change from either the
        // menu or the settings screen.
        fun applyFilters(filters: MediaFilters) =
            flow {
                val query = searchRequest.value.query

                if (filters != _filters.value) {
                    _filters.update { filters }
                    pager.reset()

                    if (query.isNotBlank()) {
                        emitAll(loadNextPage(query))
                    }
                }
            }

        fun performSearch(
            query: String,
            searchSource: SearchSource,
        ) = flow {
            val currentQuery = searchRequest.value.query

            if (query.isNotBlank() && !currentQuery.equals(query, true)) {
                // read here as well as watched by the screen, because the screen can ask for a search
                // before that first read lands, and the first page has to be narrowed like the rest
                _filters.update { searchPreferenceRepository.getFilters().first() }
                _activeSearchTerm.update { query }
                pager.reset()
                _searchRequest.update { SearchRequest(query, searchSource) }

                emitAll(loadNextPage(query))

                if (searchResults.value.isNotEmpty()) {
                    addSearchHistory(query)
                }
            }
        }

        fun continueSearch() =
            flow {
                val query = searchRequest.value.query

                if (query.isNotBlank() && hasMoreResults.value) {
                    emitAll(loadNextPage(query))
                }
            }

        // the first page failing leaves nothing that says there is more, so unlike continuing this
        // only needs there to be a search to go back to
        fun retrySearch() =
            flow {
                val query = searchRequest.value.query

                if (query.isNotBlank()) {
                    emitAll(pager.retry { offset -> fetchPage(query, offset) })
                }
            }

        private fun loadNextPage(query: String) = pager.loadNextPage { offset -> fetchPage(query, offset) }

        private suspend fun fetchPage(
            query: String,
            offset: Int,
        ) = api.search(query, offset, _filters.value.favoritesOnly, _filters.value.mediaType?.toApiFilter())

        private suspend fun addSearchHistory(term: String) {
            searchHistoryDao.addSearchTerm(
                SearchHistory(
                    term,
                    Calendar.getInstance(),
                ),
            )

            cleanSearchHistory()
        }

        private suspend fun cleanSearchHistory() {
            try {
                val historyToKeep = searchPreferenceRepository.getSearchesToSaveCount().first()
                val earliestDateToRemove = searchHistoryDao.getEarliestDateToRemove(historyToKeep)

                searchHistoryDao.removeOldHistory(earliestDateToRemove)
            } catch (t: Throwable) {
                Timber.e(t, "Error trying to clean up search history")
            }
        }
    }
