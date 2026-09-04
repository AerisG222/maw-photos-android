package us.mikeandwan.photos.ui.screens.mediaFeed

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.ApiErrorHandler
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.MediaFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.PlacePreferenceRepository
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceAncestor
import us.mikeandwan.photos.domain.models.PlaceKind
import us.mikeandwan.photos.domain.models.PlacePreference
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Media as ApiMedia

/*
   Driven against a real MediaFeedRepository over a mocked api client: the parts worth pinning here -
   the seed staying put across pages, a filter change starting over - live in how the two are wired
   together, and would be asserted into existence by a mocked repository rather than tested.
*/
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediaFeedViewModelTest {
    private lateinit var api: FaceApiClient
    private lateinit var placeApi: PlaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var mediaFeedRepository: MediaFeedRepository
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var clanRepository: ClanRepository
    private lateinit var placeRepository: PlaceRepository
    private lateinit var peoplePreferenceRepository: PeoplePreferenceRepository
    private lateinit var placePreferenceRepository: PlacePreferenceRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categoryPreferenceRepository: CategoryPreferenceRepository
    private lateinit var mediaPreferenceRepository: MediaPreferenceRepository
    private lateinit var mediaFavoriteService: MediaFavoriteService

    private val personId = Uuid.random()
    private val subject = MediaFeedSubject.Person(personId)

    private val person = Person(
        id = personId,
        name = "Alice",
        preferredFaceUrl = null,
        mediaCount = 3,
        isFavorite = false,
    )

    private val placeId = Uuid.random()
    private val placeSubject = MediaFeedSubject.Place(placeId)

    // a city, which is a leaf: tapping its tile opens this feed rather than a level of the tree, so
    // the feed is the only screen that ever shows the chain above it
    private val boston = Place(
        id = placeId,
        parentId = Uuid.random(),
        kind = PlaceKind.City,
        name = "Boston",
        mediaCount = 921,
        coverUrl = null,
        childCount = 0,
    )

    private val bostonChain = listOf(
        PlaceAncestor(Uuid.random(), PlaceKind.Country, "United States"),
        PlaceAncestor(Uuid.random(), PlaceKind.State, "Massachusetts"),
        PlaceAncestor(placeId, PlaceKind.City, "Boston"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        api = mockk()
        placeApi = mockk()
        apiErrorHandler = mockk(relaxed = true)
        mediaFeedRepository = MediaFeedRepository(api, placeApi, apiErrorHandler)

        peopleRepository = mockk(relaxed = true)
        clanRepository = mockk(relaxed = true)
        placeRepository = mockk(relaxed = true)
        peoplePreferenceRepository = mockk(relaxed = true)
        placePreferenceRepository = mockk(relaxed = true)
        peoplePreferenceRepository = mockk(relaxed = true)
        placePreferenceRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        categoryPreferenceRepository = mockk(relaxed = true)
        mediaPreferenceRepository = mockk(relaxed = true)
        mediaFavoriteService = mockk(relaxed = true)

        every { peopleRepository.people } returns MutableStateFlow(listOf(person))
        every { peopleRepository.getPeople(any()) } returns
            flowOf(ExternalCallStatus.Success(listOf(person)))
        every { categoryPreferenceRepository.getCategoryPreference() } returns
            flowOf(CategoryPreference())
        every { mediaPreferenceRepository.getPhotoGridItemSize() } returns
            flowOf(GridThumbnailSize.Medium)
        every { mediaPreferenceRepository.getMediaPreference() } returns flowOf(MediaPreference())
        // the real repository caches what it reads, which is where a place feed takes its name
        // from - see the note on the title flow
        every { placeRepository.placesById } returns MutableStateFlow(mapOf(placeId to boston))
        every { placeRepository.getPlace(any()) } returns
            flowOf(ExternalCallStatus.Success(boston))
        every { placeRepository.getAncestors(any()) } returns
            flowOf(ExternalCallStatus.Success(bostonChain))
        // the category labels are read from whichever area the subject belongs to, and a relaxed
        // mock answers with a flow that never emits - which would leave the whole state waiting
        every { peoplePreferenceRepository.getPeoplePreference() } returns flowOf(PeoplePreference())
        every { placePreferenceRepository.getPlacePreference() } returns flowOf(PlacePreference())
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

        val seed = mediaFeedRepository.filter.value.seed

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

        assertEquals(null, mediaFeedRepository.filter.value.seed)
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

        val media = mediaFeedRepository.media.value.single()

        coEvery { mediaFavoriteService.setIsFavorite(media, true) } returns true

        vm.toggleFavorite(media)
        advanceUntilIdle()

        assertTrue(mediaFeedRepository.media.value.single().isFavorite)
    }

    // ---- the categories listing ----

    @Test
    fun `a person's category labels are saved against the people area`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        val vm = viewModel()

        vm.initState(subject)
        vm.setShowCategories(true)
        advanceUntilIdle()

        // both start on: the list view has always drawn them, so a toggle only ever takes one away
        assertTrue(vm.uiState.value.categoryLabels.showYear)
        assertTrue(vm.uiState.value.categoryLabels.showTitle)

        vm.setShowCategoryYear(false)
        vm.setShowCategoryTitle(false)
        advanceUntilIdle()

        coVerify { peoplePreferenceRepository.setShowCategoryYear(false) }
        coVerify { peoplePreferenceRepository.setShowCategoryTitle(false) }
        coVerify(exactly = 0) { placePreferenceRepository.setShowCategoryYear(any()) }

        // what a category says about itself is not what came back, so nothing is asked for again
        assertEquals(3, vm.uiState.value.categories.size)
        coVerify(exactly = 1) { api.getPersonCategories(personId, 0, false) }
    }

    @Test
    fun `a place's category labels are saved against the places area instead`() = runTest {
        coEvery { placeApi.getPlaceMedia(placeId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { placeApi.getPlaceCategories(placeId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(placeSubject)
        vm.setShowCategories(true)
        advanceUntilIdle()

        vm.setShowCategoryYear(false)
        advanceUntilIdle()

        // the two areas share this feed but are read differently, so the choice is kept apart
        coVerify { placePreferenceRepository.setShowCategoryYear(false) }
        coVerify(exactly = 0) { peoplePreferenceRepository.setShowCategoryYear(any()) }
    }

    @Test
    fun `switching to the categories loads them and reports on that listing`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = true, nextOffset = 3))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        // Act
        vm.setShowCategories(true)
        advanceUntilIdle()

        // Assert
        assertTrue(vm.uiState.value.showCategories)
        assertEquals(3, vm.uiState.value.categories.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isEmpty)

        // the paging flag follows whatever is on screen, so the grid does not have to ask which
        // listing it is drawing before it can page
        assertTrue(vm.uiState.value.hasMore)
    }

    @Test
    fun `going back to the media does not ask for it a second time`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 3, hasMore = false, nextOffset = 3))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setShowCategories(true)
        advanceUntilIdle()

        // Act
        vm.setShowCategories(false)
        advanceUntilIdle()

        // Assert
        assertEquals(2, vm.uiState.value.gridItems.size)
        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 1) { api.getPersonMedia(personId, 0, false, null) }
    }

    // a person can be in nothing the caller has favorited, which is a real answer about somebody
    // they can see rather than an empty listing
    @Test
    fun `a categories listing with nothing in it reports empty`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Error("not found", java.net.HttpURLConnection.HTTP_NOT_FOUND)

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setShowCategories(true)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `favoriting a category keeps how much of it the person is in`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))
        coEvery { api.getPersonCategories(personId, 0, false) } returns
            ApiResult.Success(categoryPage(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        vm.setShowCategories(true)
        advanceUntilIdle()

        val category = mediaFeedRepository.categories.value.single()

        // the answer describes the category on its own terms, and carries no count
        every { categoryRepository.setFavorite(category.id, true) } returns
            flowOf(ExternalCallStatus.Success(category.copy(isFavorite = true, mediaCount = null)))

        // Act
        vm.toggleCategoryFavorite(category)
        advanceUntilIdle()

        // Assert
        val updated = mediaFeedRepository.categories.value.single()

        assertTrue(updated.isFavorite)
        assertEquals(4, updated.mediaCount)
    }

    @Test
    fun `a place feed carries the chain above it, which is the only screen a leaf's is drawn on`() = runTest {
        coEvery { placeApi.getPlaceMedia(placeId, 0, false, null) } returns
            ApiResult.Success(page(count = 2, hasMore = false, nextOffset = 2))

        val vm = viewModel()

        vm.initState(placeSubject)
        advanceUntilIdle()

        assertEquals("Boston", vm.uiState.value.title)
        assertEquals(
            listOf("United States", "Massachusetts", "Boston"),
            vm.uiState.value.placeChain.map { it.name },
        )
    }

    @Test
    fun `a person feed has no chain, being nowhere in the place tree`() = runTest {
        coEvery { api.getPersonMedia(personId, 0, false, null) } returns
            ApiResult.Success(page(count = 1, hasMore = false, nextOffset = 1))

        val vm = viewModel()

        vm.initState(subject)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.placeChain.isEmpty())
    }

    private fun viewModel() =
        MediaFeedViewModel(
            mediaFeedRepository,
            peopleRepository,
            clanRepository,
            placeRepository,
            categoryRepository,
            peoplePreferenceRepository,
            placePreferenceRepository,
            categoryPreferenceRepository,
            mediaPreferenceRepository,
            mediaFavoriteService,
        )

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
