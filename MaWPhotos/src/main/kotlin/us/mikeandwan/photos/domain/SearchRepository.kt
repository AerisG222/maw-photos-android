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
import us.mikeandwan.photos.domain.models.SearchRequest
import us.mikeandwan.photos.domain.models.SearchSource
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
        // again would look like a repeat of the current query and be skipped
        fun clearSearch() {
            _searchRequest.update { SearchRequest("", SearchSource.None) }
            _activeSearchTerm.update { "" }
            pager.reset()
        }

        fun performSearch(
            query: String,
            searchSource: SearchSource,
        ) = flow {
            val currentQuery = searchRequest.value.query

            if (query.isNotBlank() && !currentQuery.equals(query, true)) {
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

        private fun loadNextPage(query: String) = pager.loadNextPage { offset -> api.search(query, offset) }

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
