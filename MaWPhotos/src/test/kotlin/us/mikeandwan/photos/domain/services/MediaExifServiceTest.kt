package us.mikeandwan.photos.domain.services

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import kotlin.uuid.Uuid

class MediaExifServiceTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var service: MediaExifService

    @Before
    fun setUp() {
        mediaRepository = mockk()
        service = MediaExifService(mediaRepository)
    }

    @Test
    fun `fetchExifDetails updates exif on success`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )
        val expectedExif = JsonPrimitive("Some Exif Data")

        every { mediaRepository.getExifData(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Success(expectedExif)
        )

        // Act
        service.fetchExifDetails(media)

        // Assert
        assertEquals(expectedExif, service.exif.value)
    }

    @Test
    fun `fetchExifDetails sets null on failure`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )

        every { mediaRepository.getExifData(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Error("Error")
        )

        // Act
        service.fetchExifDetails(media)

        // Assert
        assertNull(service.exif.value)
    }
}
