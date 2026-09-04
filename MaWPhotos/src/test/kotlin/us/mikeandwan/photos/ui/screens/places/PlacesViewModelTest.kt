package us.mikeandwan.photos.ui.screens.places

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.domain.ApiErrorHandler
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.api.Place as ApiPlace
import us.mikeandwan.photos.api.PlaceAncestor as ApiPlaceAncestor

/*
   Driven against a real PlaceRepository over a mocked api client: what is worth pinning here - a
   leaf hiding its own listing, a missing place told apart from an empty one - lives in how the two
   are wired together, and a mocked repository would assert it into existence rather than test it.
*/
@OptIn(ExperimentalCoroutinesApi::class)
class PlacesViewModelTest {
    private lateinit var api: PlaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: PlaceRepository

    private val countryId = Uuid.random()
    private val stateId = Uuid.random()
    private val cityId = Uuid.random()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        api = mockk(relaxed = true)
        apiErrorHandler = mockk(relaxed = true)
        repository = PlaceRepository(api, apiErrorHandler)

        coEvery { api.getPlaces(any()) } returns ApiResult.Success(emptyList())
        coEvery { api.getPlaceAncestors(any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the root lists the countries`() = runTest {
        coEvery { api.getPlaces(null) } returns
            ApiResult.Success(listOf(place(countryId, "United States", "country", childCount = 2)))

        val vm = PlacesViewModel(repository)
        vm.setPlace(null)
        advanceUntilIdle()

        val state = vm.uiState.value

        assertNull(state.place)
        assertEquals(listOf("United States"), state.places.map { it.name })
        assertFalse(state.isLoading)
    }

    @Test
    fun `drilling in lists what is inside the place rather than the root`() = runTest {
        coEvery { api.getPlace(countryId) } returns
            ApiResult.Success(place(countryId, "United States", "country", childCount = 1))
        coEvery { api.getPlaces(countryId) } returns
            ApiResult.Success(listOf(place(stateId, "Massachusetts", "state", childCount = 2)))

        val vm = PlacesViewModel(repository)
        vm.setPlace(countryId)
        advanceUntilIdle()

        val state = vm.uiState.value

        assertEquals(countryId, state.placeId)
        assertEquals("United States", state.place?.name)
        assertEquals(listOf("Massachusetts"), state.places.map { it.name })
        assertTrue(state.showsChildren)
    }

    @Test
    fun `a leaf leaves out the listing rather than showing an empty one`() = runTest {
        coEvery { api.getPlace(cityId) } returns
            ApiResult.Success(place(cityId, "Boston", "city", childCount = 0))
        coEvery { api.getPlaceAncestors(cityId) } returns
            ApiResult.Success(listOf(ancestor(cityId, "Boston", "city", depth = 3)))

        val vm = PlacesViewModel(repository)
        vm.setPlace(cityId)
        advanceUntilIdle()

        val state = vm.uiState.value

        assertTrue(state.place?.isLeaf == true)
        assertFalse(state.showsChildren)
    }

    @Test
    fun `a place that is gone is told apart from one with nothing inside it`() = runTest {
        coEvery { api.getPlace(stateId) } returns ApiResult.Error("gone", 404)

        val vm = PlacesViewModel(repository)
        vm.setPlace(stateId)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isPlaceMissing)
    }

    @Test
    fun `the chain above a place is read whole, so a cold start into the middle of the tree has one`() = runTest {
        coEvery { api.getPlace(stateId) } returns
            ApiResult.Success(place(stateId, "Massachusetts", "state", childCount = 4))
        coEvery { api.getPlaceAncestors(stateId) } returns
            ApiResult.Success(
                listOf(
                    ancestor(countryId, "United States", "country", depth = 1),
                    ancestor(stateId, "Massachusetts", "state", depth = 2),
                ),
            )

        val vm = PlacesViewModel(repository)
        vm.setPlace(stateId)
        advanceUntilIdle()

        assertEquals(
            listOf("United States", "Massachusetts"),
            vm.uiState.value.chain.map { it.name },
        )
    }

    @Test
    fun `moving to another level starts over rather than showing the last one's children`() = runTest {
        coEvery { api.getPlaces(countryId) } returns
            ApiResult.Success(listOf(place(stateId, "Massachusetts", "state", childCount = 2)))
        coEvery { api.getPlaces(stateId) } returns
            ApiResult.Success(listOf(place(cityId, "Boston", "city", childCount = 0)))

        val vm = PlacesViewModel(repository)

        vm.setPlace(countryId)
        advanceUntilIdle()

        assertEquals(listOf("Massachusetts"), vm.uiState.value.places.map { it.name })

        vm.setPlace(stateId)
        advanceUntilIdle()

        assertEquals(listOf("Boston"), vm.uiState.value.places.map { it.name })
    }

    private fun place(
        id: Uuid,
        name: String,
        kind: String,
        childCount: Int,
    ) = ApiPlace(
        id = id,
        parentId = null,
        kind = kind,
        name = name,
        slug = name.lowercase(),
        mediaCount = 42,
        ancestorNames = emptyList(),
        coverUrl = null,
        coverMediaId = null,
        childCount = childCount,
    )

    private fun ancestor(
        id: Uuid,
        name: String,
        kind: String,
        depth: Int,
    ) = ApiPlaceAncestor(
        id = id,
        parentId = null,
        kind = kind,
        name = name,
        slug = name.lowercase(),
        depth = depth,
    )
}
