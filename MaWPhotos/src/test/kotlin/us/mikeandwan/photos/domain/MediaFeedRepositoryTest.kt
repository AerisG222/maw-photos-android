package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.HttpURLConnection
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
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Media as ApiMedia

class MediaFeedRepositoryTest {
    private lateinit var api: FaceApiClient
    private lateinit var placeApi: PlaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: MediaFeedRepository

    private val personId = Uuid.random()
    private val subject = MediaFeedSubject.Person(personId)

    @Before
    fun setUp() {
        api = mockk()
        placeApi = mockk()
        apiErrorHandler = mockk()
        repository = MediaFeedRepository(api, placeApi, apiErrorHandler)
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

        repository.setFilter(MediaFeedFilter(favoritesOnly = true))

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

    // the pager calls initialize on its way in, over the feed the grid has already narrowed.  if a
    // filter counted as part of the feed's identity that call would reset it to an unfiltered first
    // page, and the item that was tapped would no longer be in the list to show.
    @Test
    fun `re-initializing with the same subject keeps a filter the grid applied`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.getPersonMedia(personId, 0, true, 42L) } returns
            ApiResult.Success(page(count = 3, hasMore = true, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPage().toList()

        repository.setFilter(MediaFeedFilter(favoritesOnly = true, seed = 42L))
        repository.loadNextPage().toList()

        // Act
        repository.initialize(subject)

        // Assert
        assertEquals(MediaFeedFilter(favoritesOnly = true, seed = 42L), repository.filter.value)
        assertEquals(3, repository.media.value.size)
    }

    @Test
    fun `moving to another subject starts over, unfiltered`() = runTest {
        val otherId = Uuid.random()

        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.setFilter(MediaFeedFilter(favoritesOnly = true, seed = 42L))
        repository.initialize(MediaFeedSubject.Clan(otherId))

        assertTrue(repository.media.value.isEmpty())
        assertEquals(MediaFeedFilter(), repository.filter.value)
    }

    // a feed the API answers whole comes back with no more results and an offset of zero.  reading
    // that offset as "nothing loaded yet" sends the identical request again, and the same rows land
    // in the list twice - which the grid, keyed by media id, dies on rather than merely drawing
    // wrong.
    @Test
    fun `a feed answered whole in one page is not asked for again`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 0))

        repository.initialize(subject)
        repository.loadNextPage().toList()

        // Act
        repository.loadNextPage().toList()

        // Assert
        assertEquals(2, repository.media.value.size)
        assertEquals(2, repository.media.value.distinctBy { it.id }.size)
        coVerify(exactly = 1) { api.getPersonMedia(personId, 0, false, null) }
    }

    // the grid pages as it scrolls, so a request can still be in flight when the user narrows or
    // reshuffles.  appending what it returns would leave the list holding rows the new filter
    // excludes - and, once the new pages land beside them, the same media id twice, which is a
    // crash in the grid rather than a cosmetic problem.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a page still in flight when the filter changes is discarded`() = runTest {
        val stale = page(count = 2, hasMore = true, nextOffset = 2)
        val fresh = page(count = 1, hasMore = false, nextOffset = 1)
        val staleRequestLanded = CompletableDeferred<Unit>()

        coEvery { api.getPersonMedia(personId, 0, false, null) } coAnswers {
            staleRequestLanded.await()
            ApiResult.Success(stale)
        }
        coEvery { api.getPersonMedia(personId, 0, true, null) } returns ApiResult.Success(fresh)

        repository.initialize(subject)

        val staleLoad = launch { repository.loadNextPage().toList() }
        runCurrent()

        // Act - the filter changes, and only then does the first request come back
        repository.setFilter(MediaFeedFilter(favoritesOnly = true))
        repository.loadNextPage().toList()

        staleRequestLanded.complete(Unit)
        staleLoad.join()

        // Assert
        assertEquals(1, repository.media.value.size)
        assertEquals(fresh.results.first().id, repository.media.value.first().id)
    }

    // ---- the categories a subject turns up in, the feed's other listing ----

    @Test
    fun `categories page and accumulate the same way the media does`() = runTest {
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 2, false) } returns
            ApiResult.Success(categoryPage(count = 1, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPageOfCategories().toList()

        assertEquals(2, repository.categories.value.size)
        assertTrue(repository.hasMoreCategories.value)

        repository.loadNextPageOfCategories().toList()

        assertEquals(3, repository.categories.value.size)
        assertFalse(repository.hasMoreCategories.value)
    }

    // the two listings are how one subject is looked at, so moving between them is not a reason to
    // ask the API for anything already in hand
    @Test
    fun `each listing keeps what it has loaded while the other is browsed`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.loadNextPageOfCategories().toList()

        // Act - back to the media, which has nothing more to fetch
        repository.loadNextPage().toList()

        // Assert
        assertEquals(2, repository.media.value.size)
        assertEquals(3, repository.categories.value.size)
        coVerify(exactly = 1) { api.getPersonMedia(personId, 0, false, null) }
        coVerify(exactly = 1) { api.getPersonCategories(personId, 0, false) }
    }

    @Test
    fun `narrowing to favorites starts both listings over`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPage().toList()
        repository.loadNextPageOfCategories().toList()

        // Act
        repository.setFilter(MediaFeedFilter(favoritesOnly = true))

        // Assert
        assertTrue(repository.media.value.isEmpty())
        assertTrue(repository.categories.value.isEmpty())
    }

    // a seed only orders media.  there is no shuffling a list of categories - the API takes no seed
    // for one - so throwing away what has been read of it would be a fetch spent on nothing.
    @Test
    fun `reshuffling leaves the categories where they are`() = runTest {
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPageOfCategories().toList()

        // Act
        repository.setFilter(MediaFeedFilter(seed = 42L))

        // Assert
        assertEquals(3, repository.categories.value.size)
    }

    @Test
    fun `moving to another subject starts the categories over too`() = runTest {
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        repository.initialize(subject)
        repository.loadNextPageOfCategories().toList()

        // Act
        repository.initialize(MediaFeedSubject.Clan(Uuid.random()))

        // Assert
        assertTrue(repository.categories.value.isEmpty())
    }

    // the same rule the media listing follows, and for the same reason - see the notes on
    // GetPersonCategories in maw-media
    @Test
    fun `a 404 is an empty categories listing rather than an error`() = runTest {
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Error("not found", HttpURLConnection.HTTP_NOT_FOUND)

        repository.initialize(subject)

        val statuses = repository.loadNextPageOfCategories().toList()

        assertTrue(statuses.last() is ExternalCallStatus.Success)
        assertTrue(repository.categories.value.isEmpty())
        coVerify(exactly = 0) { apiErrorHandler.handleError(any(), any()) }
    }

    @Test
    fun `a clan's categories come from the clan endpoint`() = runTest {
        val clanId = Uuid.random()

        coEvery { api.getClanCategories(clanId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 1, hasMore = false, nextOffset = 1))

        repository.initialize(MediaFeedSubject.Clan(clanId))
        repository.loadNextPageOfCategories().toList()

        assertEquals(1, repository.categories.value.size)
        coVerify(exactly = 1) { api.getClanCategories(clanId, 0, false) }
    }

    private fun categoryPage(
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
