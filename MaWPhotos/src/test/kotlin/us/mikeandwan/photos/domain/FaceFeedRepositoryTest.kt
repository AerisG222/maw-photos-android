package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.HttpURLConnection
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.FaceFeedFilter
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.api.Media as ApiMedia

class FaceFeedRepositoryTest {
    private lateinit var api: FaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: FaceFeedRepository

    private val personId = Uuid.random()
    private val subject = FaceFeedSubject.Person(personId)

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk()
        repository = FaceFeedRepository(api, apiErrorHandler)
    }

    @Test
    fun `pages accumulate, picking up where the api says the next one starts`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.getPersonMedia(personId, 2, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPage().toList()

        assertEquals(2, repository.media.value.size)
        assertTrue(repository.hasMore.value)

        repository.loadNextPage().toList()

        assertEquals(3, repository.media.value.size)
        assertFalse(repository.hasMore.value)
    }

    @Test
    fun `stops asking once the api says there is no more`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.loadNextPage().toList()

        coVerify(exactly = 1) { api.getPersonMedia(personId, any(), any(), any()) }
    }

    // the api answers an empty feed with 404 rather than an empty first page, and answers the same
    // way for a person the caller cannot see - both mean there is nothing to show
    @Test
    fun `a 404 is an empty feed rather than an error`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Error("not found", HttpURLConnection.HTTP_NOT_FOUND)

        repository.initialize(subject)

        val statuses = repository.loadNextPage().toList()
        val last = statuses.last()

        assertTrue(last is ExternalCallStatus.Success)
        assertEquals(emptyList<Any>(), (last as ExternalCallStatus.Success).result)
        assertTrue(repository.media.value.isEmpty())
        assertFalse(repository.hasMore.value)

        // nothing was reported to the user, because nothing went wrong
        coVerify(exactly = 0) { apiErrorHandler.handleError(any(), any()) }
    }

    @Test
    fun `any other failure is reported`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns ApiResult.Error("boom", 500)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("boom")

        repository.initialize(subject)

        val statuses = repository.loadNextPage().toList()

        assertTrue(statuses.last() is ExternalCallStatus.Error)
        coVerify(exactly = 1) { apiErrorHandler.handleError(any(), any()) }
    }

    @Test
    fun `changing the filter starts the feed over`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.getPersonMedia(personId, 0, true, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        repository.initialize(subject)
        repository.loadNextPage().toList()

        assertEquals(2, repository.media.value.size)

        repository.setFilter(FaceFeedFilter(favoritesOnly = true))

        // the previous subject's rows are gone the moment the filter changes, rather than lingering
        // under a filter they may not match
        assertTrue(repository.media.value.isEmpty())

        repository.loadNextPage().toList()

        assertEquals(1, repository.media.value.size)
    }

    @Test
    fun `re-initializing with the same subject keeps what has been loaded`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.initialize(subject)

        assertEquals(2, repository.media.value.size)
    }

    @Test
    fun `moving to another subject starts over`() = runTest {
        val otherId = Uuid.random()

        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.initialize(FaceFeedSubject.Clan(otherId))

        assertTrue(repository.media.value.isEmpty())
    }

    private fun page(
        count: Int,
        hasMore: Boolean,
        nextOffset: Int,
    ) = SearchResults(
        results = (0 until count).map {
            ApiMedia(
                id = Uuid.random(),
                categoryId = Uuid.random(),
                type = "photo",
                isFavorite = false,
            )
        },
        hasMoreResults = hasMore,
        nextOffset = nextOffset,
    )
}
