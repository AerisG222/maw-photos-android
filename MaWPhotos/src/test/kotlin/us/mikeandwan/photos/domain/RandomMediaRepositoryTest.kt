package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.api.Media as ApiMedia

@OptIn(ExperimentalCoroutinesApi::class)
class RandomMediaRepositoryTest {
    private lateinit var api: MediaApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: RandomMediaRepository

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk()
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("nope")
        repository = RandomMediaRepository(api, apiErrorHandler)
    }

    // each request draws from the whole library, so repeats are expected, and a grid keyed by id
    // cannot hold the same id twice
    @Test
    fun `media a later page repeats is not added again`() = runTest {
        val repeated = media()

        coEvery { api.getRandomMedia(any()) } returnsMany listOf(
            ApiResult.Success(listOf(repeated, media())),
            ApiResult.Success(listOf(media(), repeated)),
        )

        repository.loadNextPage().toList()
        repository.loadNextPage().toList()

        assertEquals(3, repository.media.value.size)
        assertEquals(3, repository.media.value.distinctBy { it.id }.size)
    }

    @Test
    fun `only one page is requested however many times it is asked for`() = runTest {
        val page = CompletableDeferred<ApiResult<List<ApiMedia>>>()

        coEvery { api.getRandomMedia(any()) } coAnswers { page.await() }

        val first = launch { repository.loadNextPage().toList() }
        val second = launch { repository.loadNextPage().toList() }
        runCurrent()

        page.complete(ApiResult.Success(listOf(media())))
        first.join()
        second.join()

        coVerify(exactly = 1) { api.getRandomMedia(any()) }
    }

    @Test
    fun `the next page can be asked for once the last has landed`() = runTest {
        coEvery { api.getRandomMedia(any()) } answers { ApiResult.Success(listOf(media())) }

        repository.loadNextPage().toList()
        repository.loadNextPage().toList()

        assertEquals(2, repository.media.value.size)
    }

    // the widget fetches on its own schedule, and its photo does not belong on the Random screen
    @Test
    fun `a one-off fetch answers with media but leaves the listing alone`() = runTest {
        coEvery { api.getRandomMedia(1) } returns ApiResult.Success(listOf(media()))

        val statuses = repository.fetch(1).toList()

        assertTrue(statuses.last() is ExternalCallStatus.Success)
        assertEquals(0, repository.media.value.size)
    }

    @Test
    fun `a failed page stops loading until a retry`() = runTest {
        coEvery { api.getRandomMedia(any()) } returnsMany listOf(
            ApiResult.Error("nope", 500),
            ApiResult.Success(listOf(media())),
        )

        repository.loadNextPage().toList()
        assertTrue(repository.loadFailed.value)

        repository.loadNextPage().toList()
        coVerify(exactly = 1) { api.getRandomMedia(any()) }

        repository.retryNextPage().toList()
        assertFalse(repository.loadFailed.value)
        assertEquals(1, repository.media.value.size)
    }

    private fun media() =
        ApiMedia(
            id = Uuid.random(),
            categoryId = Uuid.random(),
            type = "photo",
            isFavorite = false,
        )
}
