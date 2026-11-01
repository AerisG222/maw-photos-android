package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.database.SearchHistoryDao
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.domain.models.SearchSource
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Media as ApiMedia

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryTest {
    private lateinit var api: CategoryApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: SearchRepository

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk(relaxed = true)
        repository = SearchRepository(
            api,
            mockk<SearchHistoryDao>(relaxed = true),
            mockk<SearchPreferenceRepository>(relaxed = true),
            apiErrorHandler,
        )
    }

    @Test
    fun `continuing picks up where the api says the next page starts`() = runTest {
        coEvery { api.search("beach", 0) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.search("beach", 2) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0))

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        repository.continueSearch().toList()

        assertEquals(3, repository.searchResults.value.size)
        assertFalse(repository.hasMoreResults.value)
    }

    // the api orders tied search ranks arbitrarily, so a page can repeat a row from the one before,
    // and a grid keyed by id crashes on a repeat
    @Test
    fun `a category a later page repeats is not added again`() = runTest {
        val firstPage = page(count = 2, hasMore = true, nextOffset = 2)
        val secondPage = page(count = 1, hasMore = false, nextOffset = 0)

        coEvery { api.search("beach", 0) } returns ApiResult.Success(firstPage)
        coEvery { api.search("beach", 2) } returns
            ApiResult.Success(secondPage.copy(results = firstPage.results.takeLast(1) + secondPage.results))

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        repository.continueSearch().toList()

        assertEquals(3, repository.searchResults.value.size)
        assertEquals(3, repository.searchResults.value.distinctBy { it.id }.size)
    }

    // scrolling asks again as each row passes, and the same page twice would put duplicate ids in
    // a grid keyed by them
    @Test
    fun `only one request runs however many times the next page is asked for`() = runTest {
        val secondPage = CompletableDeferred<ApiResult<SearchResults<ApiCategory>>>()

        coEvery { api.search("beach", 0) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.search("beach", 2) } coAnswers { secondPage.await() }

        repository.performSearch("beach", SearchSource.SearchMenu).toList()

        val first = launch { repository.continueSearch().toList() }
        val second = launch { repository.continueSearch().toList() }
        runCurrent()

        secondPage.complete(ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0)))
        first.join()
        second.join()

        coVerify(exactly = 1) { api.search("beach", 2) }
        assertEquals(3, repository.searchResults.value.size)
    }

    @Test
    fun `a page still in flight is dropped once a new search starts`() = runTest {
        val stalePage = CompletableDeferred<ApiResult<SearchResults<ApiCategory>>>()

        coEvery { api.search("beach", 0) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.search("beach", 2) } coAnswers { stalePage.await() }
        coEvery { api.search("snow", 0) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0))

        repository.performSearch("beach", SearchSource.SearchMenu).toList()

        val continuing = launch { repository.continueSearch().toList() }
        runCurrent()

        repository.performSearch("snow", SearchSource.SearchMenu).toList()

        stalePage.complete(ApiResult.Success(page(count = 5, hasMore = true, nextOffset = 7)))
        continuing.join()

        assertEquals(1, repository.searchResults.value.size)
        assertFalse(repository.hasMoreResults.value)
    }

    // being near the end is still true after a failure, so asking again from there would go on for
    // as long as the server kept failing
    @Test
    fun `a failed page stops continuing until a retry, and the retry picks up where it left off`() = runTest {
        coEvery { api.search("beach", 0) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.search("beach", 2) } returnsMany listOf(
            ApiResult.Error("nope", 500),
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0)),
        )

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        repository.continueSearch().toList()

        assertTrue(repository.loadFailed.value)
        assertEquals(2, repository.searchResults.value.size)

        repository.continueSearch().toList()

        coVerify(exactly = 1) { api.search("beach", 2) }

        repository.retrySearch().toList()

        assertFalse(repository.loadFailed.value)
        assertEquals(3, repository.searchResults.value.size)
    }

    @Test
    fun `a first page that failed can be retried, though nothing says there is more`() = runTest {
        coEvery { api.search("beach", 0) } returnsMany listOf(
            ApiResult.Error("nope", 500),
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0)),
        )

        repository.performSearch("beach", SearchSource.SearchMenu).toList()

        assertTrue(repository.loadFailed.value)

        repository.retrySearch().toList()

        assertEquals(1, repository.searchResults.value.size)
    }

    @Test
    fun `changing filters starts the search over, asking the api to narrow it`() = runTest {
        coEvery { api.search("beach", 0) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.search("beach", 0, true, "video") } returns
            ApiResult.Success(page(count = 1, hasMore = true, nextOffset = 1))
        coEvery { api.search("beach", 1, true, "video") } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0))

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        repository
            .setFilters(MediaFilters(favoritesOnly = true, mediaType = MediaType.Video))
            .toList()

        assertEquals(1, repository.searchResults.value.size)

        // the pages after the first have to stay narrowed too
        repository.continueSearch().toList()

        assertEquals(2, repository.searchResults.value.size)
    }

    @Test
    fun `filters set before searching apply to the search, and clearing it drops them`() = runTest {
        coEvery { api.search("beach", 0, true, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0))

        repository.setFilters(MediaFilters(favoritesOnly = true)).toList()
        coVerify(exactly = 0) { api.search(any(), any(), any(), any()) }

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        assertEquals(1, repository.searchResults.value.size)

        repository.clearSearch()
        assertEquals(MediaFilters(), repository.filters.value)
    }

    @Test
    fun `a new search clears a failure from the last one`() = runTest {
        coEvery { api.search("beach", 0) } returns ApiResult.Error("nope", 500)
        coEvery { api.search("snow", 0) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 0))

        repository.performSearch("beach", SearchSource.SearchMenu).toList()
        repository.performSearch("snow", SearchSource.SearchMenu).toList()

        assertFalse(repository.loadFailed.value)
        assertEquals(1, repository.searchResults.value.size)
    }

    private fun page(
        count: Int,
        hasMore: Boolean,
        nextOffset: Int,
    ) = SearchResults(
        results = (0 until count).map {
            ApiCategory(
                id = Uuid.random(),
                name = "Category $it",
                effectiveDate = LocalDate(2024, 1, 1),
                modified = Instant.fromEpochMilliseconds(0),
                isFavorite = false,
                teaser = ApiMedia(
                    id = Uuid.random(),
                    categoryId = Uuid.random(),
                    type = "photo",
                    isFavorite = false,
                ),
                mediaTypes = listOf("photo"),
                mediaCount = 4,
            )
        },
        hasMoreResults = hasMore,
        nextOffset = nextOffset,
    )
}
