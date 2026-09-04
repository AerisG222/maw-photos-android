package us.mikeandwan.photos.domain

import java.net.HttpURLConnection
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.ExternalCallStatus

/**
 * One of a feed's listings, accumulated a page at a time.
 *
 * The media a person or clan appears in and the categories they turn up in are two lists over the
 * one subject, and everything about paging them is the same: where the next page starts, the guards
 * against asking for it twice, and throwing away what a superseded request answers. That lives here
 * once so the two listings cannot drift apart.
 *
 * [idOf] is what an item is replaced by when something about it changes - a favorite toggled from
 * the screen showing it.
 */
internal class MediaFeedPager<TApi, TDomain>(
    private val apiErrorHandler: ApiErrorHandler,
    private val errorMessage: String,
    private val idOf: (TDomain) -> Uuid,
    private val toDomain: (TApi) -> TDomain,
) {
    private val _items = MutableStateFlow<List<TDomain>>(emptyList())
    val items = _items.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore = _hasMore.asStateFlow()

    private var nextOffset = 0

    // whether anything has come back yet, which is what tells an exhausted listing apart from one
    // that has not started.  it cannot be inferred from nextOffset: a listing the API answers whole
    // in one page comes back with no more results and an offset of zero, and reading that as "not
    // started" sends the very same request again and appends the same rows twice.
    private var hasLoadedAPage = false

    // a grid can ask for the next page more than once before the first ask lands - two rows of a
    // scroll are enough - and a second request at the same offset would append the same page twice
    private var isLoading = false

    // bumped every time the listing starts over.  a request already in flight when that happens was
    // asked under the old subject or filter, and its rows do not belong in the new list - appending
    // them anyway leaves the grid holding items the filter excludes, and duplicate ids once the new
    // pages arrive alongside them.
    private var generation = 0

    /**
     * Asks [fetch] for the page after whatever has been accumulated, and appends what it answers.
     *
     * The request is a parameter rather than something held here because which one answers depends
     * on the subject and filter the feed is pointed at when the page is asked for.
     */
    fun loadNextPage(fetch: suspend (offset: Int) -> ApiResult<SearchResults<TApi>>) =
        flow {
            if (isLoading || (hasLoadedAPage && !_hasMore.value)) {
                return@flow
            }

            isLoading = true
            val generationAsked = generation
            emit(ExternalCallStatus.Loading)

            try {
                val result = fetch(nextOffset)

                // the listing started over while this was in flight, so what came back answers a
                // question nobody is asking any more
                if (generationAsked != generation) {
                    return@flow
                }

                when (result) {
                    is ApiResult.Success -> {
                        emit(handleResults(result.result))
                    }

                    // an empty listing answers 404 rather than an empty first page - see the notes
                    // on GetPersonMedia in maw-media.  a person the caller cannot see answers the
                    // same way on purpose, so both land here as simply nothing to show.
                    is ApiResult.Error -> {
                        if (result.errorCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            _hasMore.update { false }

                            emit(ExternalCallStatus.Success(emptyList()))
                        } else {
                            emit(apiErrorHandler.handleError(result, errorMessage))
                        }
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, errorMessage))
                    }
                }
            } finally {
                // a superseded request leaves the flag to whichever request replaced it
                if (generationAsked == generation) {
                    isLoading = false
                }
            }
        }

    fun update(updated: TDomain) {
        _items.update { currentList ->
            val index = currentList.indexOfFirst { idOf(it) == idOf(updated) }

            if (index < 0) {
                currentList
            } else {
                currentList.toMutableList().also { it[index] = updated }
            }
        }
    }

    fun reset() {
        generation++
        isLoading = false
        hasLoadedAPage = false
        _items.update { emptyList() }
        _hasMore.update { false }
        nextOffset = 0
    }

    private fun handleResults(results: SearchResults<TApi>): ExternalCallStatus<List<TDomain>> {
        val page = results.results.map(toDomain)

        _items.update { it + page }
        _hasMore.update { results.hasMoreResults }
        nextOffset = results.nextOffset
        hasLoadedAPage = true

        return ExternalCallStatus.Success(page)
    }
}
