package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.HttpURLConnection
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.domain.models.ClanResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.api.Clan as ApiClan

class ClanRepositoryTest {
    private lateinit var api: FaceApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: ClanRepository

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk()
        repository = ClanRepository(api, apiErrorHandler)
    }

    @Test
    fun `clans are held in name order`() = runTest {
        coEvery { api.getClans() } returns
            ApiResult.Success(listOf(apiClan(name = "Wolves"), apiClan(name = "Bears")))

        repository.getClans().toList()

        assertEquals(listOf("Bears", "Wolves"), repository.clans.value.map { it.name })
    }

    @Test
    fun `a caller with no clans yet is not an error`() = runTest {
        coEvery { api.getClans() } returns ApiResult.Empty

        val statuses = repository.getClans().toList()

        assertTrue(statuses.last() is ExternalCallStatus.Success)
        coVerify(exactly = 0) { apiErrorHandler.handleEmpty(any(), any()) }
    }

    @Test
    fun `a created clan lands in the list in name order`() = runTest {
        coEvery { api.getClans() } returns ApiResult.Success(listOf(apiClan(name = "Bears")))
        coEvery { api.createClan("Ants", any()) } returns ApiResult.Success(apiClan(name = "Ants"))

        repository.getClans().toList()

        val result = repository.createClan("Ants", listOf(Uuid.random()))

        assertTrue(result is ClanResult.Success)
        assertEquals(listOf("Ants", "Bears"), repository.clans.value.map { it.name })
    }

    @Test
    fun `a name already in use is reported as such and not as a failure`() = runTest {
        coEvery { api.createClan(any(), any()) } returns
            ApiResult.Error("taken", HttpURLConnection.HTTP_CONFLICT)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("taken")

        val result = repository.createClan("Bears", emptyList())

        assertEquals(ClanResult.DuplicateName, result)

        // logged, but without the app wide snackbar - the dialog that asked says this in place
        coVerify(exactly = 1) { apiErrorHandler.handleError(any(), null) }
    }

    @Test
    fun `a refused request is reported as invalid`() = runTest {
        coEvery { api.setClanPeople(any(), any()) } returns
            ApiResult.Error("who?", HttpURLConnection.HTTP_BAD_REQUEST)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("who?")

        val result = repository.setClanPeople(Uuid.random(), listOf(Uuid.random()))

        assertEquals(ClanResult.Invalid, result)
    }

    @Test
    fun `anything else is a plain failure and is surfaced`() = runTest {
        coEvery { api.renameClan(any(), any()) } returns ApiResult.Error("boom", 500)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("boom")

        val result = repository.renameClan(Uuid.random(), "Bears")

        assertEquals(ClanResult.Failed, result)
        coVerify(exactly = 1) { apiErrorHandler.handleError(any(), any<String>()) }
    }

    // a delete answers 204, which carries no body and so arrives as Empty
    @Test
    fun `a delete with no content is a success and drops the clan`() = runTest {
        val clan = apiClan(name = "Bears")

        coEvery { api.getClans() } returns ApiResult.Success(listOf(clan))
        coEvery { api.deleteClan(clan.id) } returns ApiResult.Empty

        repository.getClans().toList()

        assertTrue(repository.deleteClan(clan.id))
        assertTrue(repository.clans.value.isEmpty())
    }

    @Test
    fun `a failed delete leaves the clan in place`() = runTest {
        val clan = apiClan(name = "Bears")

        coEvery { api.getClans() } returns ApiResult.Success(listOf(clan))
        coEvery { api.deleteClan(clan.id) } returns
            ApiResult.Error("gone", HttpURLConnection.HTTP_NOT_FOUND)
        coEvery { apiErrorHandler.handleError(any(), any()) } returns ExternalCallStatus.Error("gone")

        repository.getClans().toList()

        assertEquals(false, repository.deleteClan(clan.id))
        assertEquals(1, repository.clans.value.size)
    }

    private fun apiClan(name: String) = ApiClan(id = Uuid.random(), name = name, members = emptyList())
}
