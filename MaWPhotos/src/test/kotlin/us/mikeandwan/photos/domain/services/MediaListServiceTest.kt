package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import kotlin.uuid.Uuid
import java.io.File

class MediaListServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var fileRepository: FileStorageRepository
    private lateinit var mediaFavoriteService: MediaFavoriteService
    private lateinit var mediaCommentService: MediaCommentService
    private lateinit var mediaExifService: MediaExifService
    private lateinit var service: MediaListService

    @Before
    fun setUp() {
        categoryRepository = mockk(relaxed = true)
        fileRepository = mockk()
        mediaFavoriteService = mockk(relaxed = true)
        mediaCommentService = mockk(relaxed = true)
        mediaExifService = mockk(relaxed = true)

        service = MediaListService(
            categoryRepository,
            fileRepository,
            mediaFavoriteService,
            mediaCommentService,
            mediaExifService
        )
    }

    @Test
    fun `initialize updates state with source media`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val mediaList = listOf(
            Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false)
        )
        val sourceMedia = MutableStateFlow(mediaList)
        val duration = MutableStateFlow(5000L)

        // Act
        service.initialize(sourceMedia, duration)

        // Assert
        assertEquals(mediaList, service.state.value.media)
    }

    @Test
    fun `SetActiveId action updates activeId in state`() = runTest {
        // Arrange
        val mediaId = Uuid.random()

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        assertEquals(mediaId, service.state.value.activeId)
    }

    @Test
    fun `ToggleShowDetails action toggles showDetailSheet`() = runTest {
        // Arrange
        assertFalse(service.state.value.showDetailSheet)

        // Act
        service.onAction(MediaListAction.ToggleShowDetails)

        // Assert
        assertTrue(service.state.value.showDetailSheet)

        // Act
        service.onAction(MediaListAction.ToggleShowDetails)

        // Assert
        assertFalse(service.state.value.showDetailSheet)
    }

    @Test
    fun `Reset action clears state`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        service.onAction(MediaListAction.SetActiveId(mediaId))
        service.onAction(MediaListAction.ToggleShowDetails)

        // Act
        service.onAction(MediaListAction.Reset)

        // Assert
        assertEquals(Uuid.NIL, service.state.value.activeId)
        assertFalse(service.state.value.showDetailSheet)
        assertTrue(service.state.value.media.isEmpty())
    }

    @Test
    fun `SaveFileToShare action calls repository and triggers callback`() = runTest {
        // Arrange
        val drawable = mockk<Drawable>()
        val filename = "test.jpg"
        val expectedFile = mockk<File>()
        var capturedFile: File? = null

        coEvery { fileRepository.savePhotoToShare(drawable, filename) } returns expectedFile

        // Act
        service.onAction(MediaListAction.SaveFileToShare(drawable, filename) {
            capturedFile = it
        })

        // Assert
        coVerify { fileRepository.savePhotoToShare(drawable, filename) }
        assertEquals(expectedFile, capturedFile)
    }
}
