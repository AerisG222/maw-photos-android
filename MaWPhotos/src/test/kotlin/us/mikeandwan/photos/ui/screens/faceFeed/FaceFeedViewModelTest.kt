package us.mikeandwan.photos.ui.screens.faceFeed

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.ApiErrorHandler
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.api.Media as ApiMedia

/*
   Driven against a real FaceFeedRepository over a mocked api client: the parts worth pinning here -
   the seed staying put across pages, a filter change starting over - live in how the two are wired
   together, and would be asserted into existence by a mocked repository rather than tested.
*/
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FaceFeedViewModelTest {
    private lateinit var api: FaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var faceFeedRepository: FaceFeedRepository
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var clanRepository: ClanRepository
    private lateinit var mediaPreferenceRepository: MediaPreferenceRepository
    private lateinit var mediaFavoriteService: MediaFavoriteService

    private val personId = Uuid.random()
    private val subject = FaceFeedSubject.Person(personId)

    private val person = Person(
        id = personId,
        name = "Alice",
        preferredFaceUrl = null,
        mediaCount = 3,
        isFavorite = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        api = mockk()
        apiErrorHandler = mockk(relaxed = true)
        faceFeedRepository = FaceFeedRepository(api, apiErrorHandler)

        peopleRepository = mockk(relaxed = true)
        clanRepository = mockk(relaxed = true)
        mediaPreferenceRepository = mockk(relaxed = true)
        mediaFavoriteService = mockk(relaxed = true)

        every { peopleRepository.people } returns MutableStateFlow(listOf(person))
        every { peopleRepository.getPeople(any()) } returns
            flowOf(ExternalCallStatus.Success(listOf(person)))
        every { mediaPreferenceRepository.getPhotoGridItemSize() } returns
            flowOf(GridThumbnailSize.Medium)
        every { mediaPreferenceRepository.getMediaPreference() } returns flowOf(MediaPreference())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening a feed loads the first page and titles itself with the person`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        assertEquals("Alice", vm.uiState.value.title)
        assertEquals(2, vm.uiState.value.gridItems.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isEmpty)
    }

    @Test
    fun `a feed with nothing in it reports empty rather than loading forever`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Error("not found", java.net.HttpURLConnection.HTTP_NOT_FOUND)

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `shuffling keeps one seed across pages`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))
        coEvery { api.getPersonMedia(personId, any(), false, any()) } returns
            ApiResult.Success(page(count = 2, hasMore = true, nextOffset = 2))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setShuffled(true)
        advanceUntilIdle()

        val seed = faceFeedRepository.filter.value.seed

        assertTrue(seed != null)

        vm.loadNextPage()
        advanceUntilIdle()

        // the second page must carry the same seed, or it would repeat and skip rows from the first
        coVerify { api.getPersonMedia(personId, 2, false, seed) }
    }

    @Test
    fun `unshuffling drops the seed`() = runTest {
        coEvery { api.getPersonMedia(personId, any(), any(), any()) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setShuffled(true)
        advanceUntilIdle()
        vm.setShuffled(false)
        advanceUntilIdle()

        assertEquals(null, faceFeedRepository.filter.value.seed)
        assertFalse(vm.uiState.value.isShuffled)
    }

    @Test
    fun `narrowing to favorites starts the feed over under the new filter`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 3, hasMore = false, nextOffset = 3))
        coEvery { api.getPersonMedia(personId, 0, true, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.gridItems.size)

        vm.setFavoritesOnly(true)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.gridItems.size)
        assertTrue(vm.uiState.value.favoritesOnly)
    }

    // an empty favorites filter is a real answer about a person the caller can see
    @Test
    fun `no favorites in a feed is reported without losing the filter`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 3, hasMore = false, nextOffset = 3))
        coEvery { api.getPersonMedia(personId, 0, true, null) } returns
            ApiResult.Success(page(count = 0, hasMore = false, nextOffset = 0))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setFavoritesOnly(true)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
        assertTrue(vm.uiState.value.favoritesOnly)
    }

    @Test
    fun `toggling a favorite patches the item in the feed`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        val media = faceFeedRepository.media.value.single()

        coEvery { mediaFavoriteService.setIsFavorite(media, true) } returns true

        vm.toggleFavorite(media)
        advanceUntilIdle()

        assertTrue(faceFeedRepository.media.value.single().isFavorite)
    }

    private fun viewModel() =
        FaceFeedViewModel(
            faceFeedRepository,
            peopleRepository,
            clanRepository,
            mediaPreferenceRepository,
            mediaFavoriteService,
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
