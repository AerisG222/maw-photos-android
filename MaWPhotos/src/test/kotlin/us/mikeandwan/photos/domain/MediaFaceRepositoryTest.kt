package us.mikeandwan.photos.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.api.Face as ApiFace

class MediaFaceRepositoryTest {
    private lateinit var api: MediaApiClient
    private lateinit var apiErrorHandler: ApiErrorHandler
    private lateinit var repository: MediaFaceRepository

    private val mediaId = Uuid.random()

    @Before
    fun setUp() {
        api = mockk()
        apiErrorHandler = mockk()
        repository = MediaFaceRepository(api, apiErrorHandler)
    }

    @Test
    fun `faces are fetched once and then served from the cache`() = runTest {
        coEvery { api.getFaces(mediaId) } returns ApiResult.Success(listOf(apiFace()))

        val first = repository.getFaces(mediaId).toList().last()
        val second = repository.getFaces(mediaId).toList()

        assertTrue(first is ExternalCallStatus.Success)
        assertEquals(1, (first as ExternalCallStatus.Success).result.size)

        // straight to the answer, without even a loading emission
        assertEquals(1, second.size)
        coVerify(exactly = 1) { api.getFaces(mediaId) }
    }

    @Test
    fun `an unassigned face keeps a null person`() = runTest {
        coEvery { api.getFaces(mediaId) } returns ApiResult.Success(listOf(apiFace(personId = null)))

        val result = repository.getFaces(mediaId).toList().last() as ExternalCallStatus.Success

        assertNull(result.result.single().personId)
    }

    @Test
    fun `boxes survive the trip unrounded`() = runTest {
        coEvery { api.getFaces(mediaId) } returns
            ApiResult.Success(listOf(apiFace(boxX = 0.125f, boxY = 0.25f)))

        val result = repository.getFaces(mediaId).toList().last() as ExternalCallStatus.Success
        val face = result.result.single()

        assertEquals(0.125f, face.boxX, 0f)
        assertEquals(0.25f, face.boxY, 0f)
    }

    @Test
    fun `clearing drops the cache`() = runTest {
        coEvery { api.getFaces(mediaId) } returns ApiResult.Success(listOf(apiFace()))

        repository.getFaces(mediaId).toList()
        repository.clear()
        repository.getFaces(mediaId).toList()

        coVerify(exactly = 2) { api.getFaces(mediaId) }
    }

    private fun apiFace(
        personId: Uuid? = Uuid.random(),
        boxX: Float = 0.1f,
        boxY: Float = 0.2f,
    ) = ApiFace(
        id = Uuid.random(),
        personId = personId,
        boxX = boxX,
        boxY = boxY,
        boxWidth = 0.3f,
        boxHeight = 0.4f,
    )
}
