package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.PlaceKind
import us.mikeandwan.photos.api.Place as ApiPlace

class PlaceRepositoryTest {
    private lateinit var api: PlaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: PlaceRepository

    private val countryId = Uuid.random()
    private val stateId = Uuid.random()

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk(relaxed = true)
        repository = PlaceRepository(api, apiErrorHandler)
    }

    @Test
    fun `only the root listing describes the countries`() = runTest {
        coEvery { api.getPlaces(null) } returns
            ApiResult.Success(listOf(place(id = countryId, name = "United States", kind = "country")))
        coEvery { api.getPlaces(countryId) } returns
            ApiResult.Success(listOf(place(name = "Massachusetts", kind = "state")))

        repository.getPlaces().toList()

        assertEquals(listOf("United States"), repository.countries.value.map { it.name })

        // drilling in answers about one country rather than about the root, and must not leave the
        // rail listing whatever level was last opened
        repository.getPlaces(countryId).toList()

        assertEquals(listOf("United States"), repository.countries.value.map { it.name })
    }

    @Test
    fun `every place that comes back is remembered, whichever read it came from`() = runTest {
        coEvery { api.getPlaces(null) } returns
            ApiResult.Success(listOf(place(id = countryId, name = "United States", kind = "country")))
        coEvery { api.getPlace(stateId) } returns
            ApiResult.Success(place(id = stateId, name = "Massachusetts", kind = "state"))

        repository.getPlaces().toList()
        repository.getPlace(stateId).toList()

        assertEquals(setOf(countryId, stateId), repository.placesById.value.keys)
        assertEquals(PlaceKind.State, repository.placesById.value[stateId]?.kind)
    }

    @Test
    fun `a place that is gone is reported without being mistaken for an empty one`() = runTest {
        coEvery { api.getPlace(stateId) } returns ApiResult.Error("gone", 404)

        val statuses = repository.getPlace(stateId).toList()

        assertTrue(statuses.last() is ExternalCallStatus.Error)
        assertTrue(repository.placesById.value.isEmpty())
    }

    @Test
    fun `a level with nothing in it is an answer rather than a failure`() = runTest {
        coEvery { api.getPlaces(countryId) } returns ApiResult.Empty

        val statuses = repository.getPlaces(countryId).toList()

        assertEquals(ExternalCallStatus.Success(emptyList<Nothing>()), statuses.last())
    }

    private fun place(
        id: Uuid = Uuid.random(),
        name: String,
        kind: String,
    ) = ApiPlace(
        id = id,
        parentId = null,
        kind = kind,
        name = name,
        slug = name.lowercase(),
        mediaCount = 12,
        ancestorNames = emptyList(),
        coverUrl = null,
        coverMediaId = null,
        childCount = 3,
    )
}
