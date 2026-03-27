package us.mikeandwan.photos.domain.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.api.Media as ApiMedia
import kotlin.uuid.Uuid

class MediaFavoriteServiceTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var service: MediaFavoriteService

    @Before
    fun setUp() {
        mediaRepository = mockk()
        service = MediaFavoriteService(mediaRepository)
    }

    @Test
    fun `setIsFavorite returns new value on success`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val categoryId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = categoryId,
            type = MediaType.Photo,
            isFavorite = false
        )
        val newFavoriteStatus = true

        val apiMediaResult = ApiMedia(
            id = mediaId,
            categoryId = categoryId,
            type = "Photo",
            isFavorite = newFavoriteStatus
        )

        every { mediaRepository.setFavorite(mediaId, newFavoriteStatus) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Success(apiMediaResult)
        )

        // Act
        val result = service.setIsFavorite(media, newFavoriteStatus)

        // Assert
        assertEquals(newFavoriteStatus, result)
        assertEquals(newFavoriteStatus, service.isFavorite.value)
        verify { mediaRepository.setFavorite(mediaId, newFavoriteStatus) }
    }

    @Test
    fun `setIsFavorite returns original value on failure`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val originalFavoriteStatus = false
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = originalFavoriteStatus
        )
        val newFavoriteStatus = true

        every { mediaRepository.setFavorite(mediaId, newFavoriteStatus) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Error("Error")
        )

        // Act
        val result = service.setIsFavorite(media, newFavoriteStatus)

        // Assert
        assertEquals(originalFavoriteStatus, result)
        assertEquals(originalFavoriteStatus, service.isFavorite.value)
        verify { mediaRepository.setFavorite(mediaId, newFavoriteStatus) }
    }
}
