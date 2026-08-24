package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.api.Person as ApiPerson

class PeopleRepositoryTest {
    private lateinit var api: FaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: PeopleRepository

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk()
        repository = PeopleRepository(api, apiErrorHandler)
    }

    @Test
    fun `getPeople loads the list and caches it`() = runTest {
        val person = apiPerson(name = "Alice")

        coEvery { api.getPeople() } returns ApiResult.Success(listOf(person))

        val statuses = repository.getPeople().toList()
        val loaded = statuses.last() as ExternalCallStatus.Success

        assertEquals(1, loaded.result.size)
        assertEquals("Alice", loaded.result.first().name)
        assertEquals(loaded.result, repository.people.value)

        // a second read is answered from the cache rather than the api
        val cached = repository.getPeople().toList()

        assertEquals(1, cached.size)
        assertTrue(cached.single() is ExternalCallStatus.Success)
        coVerify(exactly = 1) { api.getPeople() }
    }

    @Test
    fun `getPeople refetches when forced`() = runTest {
        coEvery { api.getPeople() } returns ApiResult.Success(listOf(apiPerson()))

        repository.getPeople().toList()
        repository.getPeople(forceRefresh = true).toList()

        coVerify(exactly = 2) { api.getPeople() }
    }

    @Test
    fun `setIsFavorite patches the cached person`() = runTest {
        val person = apiPerson(isFavorite = false)

        coEvery { api.getPeople() } returns ApiResult.Success(listOf(person))
        coEvery { api.setPersonFavorite(person.id, true) } returns
            ApiResult.Success(person.copy(isFavorite = true))

        repository.getPeople().toList()

        val result = repository.setIsFavorite(repository.people.value.single(), true)

        assertTrue(result)
        assertTrue(repository.people.value.single().isFavorite)
    }

    @Test
    fun `setIsFavorite keeps the value the person already had when the call fails`() = runTest {
        val person = apiPerson(isFavorite = true)

        coEvery { api.getPeople() } returns ApiResult.Success(listOf(person))
        coEvery { api.setPersonFavorite(person.id, false) } returns ApiResult.Error("nope", 500)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("nope")

        repository.getPeople().toList()

        val result = repository.setIsFavorite(repository.people.value.single(), false)

        assertTrue(result)
        assertTrue(repository.people.value.single().isFavorite)
    }

    private fun apiPerson(
        name: String = "Alice",
        isFavorite: Boolean = false,
    ) = ApiPerson(
        id = Uuid.random(),
        name = name,
        slug = null,
        preferredFaceId = null,
        preferredFaceUrl = null,
        mediaCount = 3,
        isFavorite = isFavorite,
    )
}
