package us.mikeandwan.photos.domain.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import kotlin.uuid.Uuid

class MediaCommentServiceTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var service: MediaCommentService

    @Before
    fun setUp() {
        mediaRepository = mockk()
        service = MediaCommentService(mediaRepository)
    }

    @Test
    fun `fetchCommentDetails updates comments on success`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )
        val expectedComments = listOf(
            Comment(
                commentId = Uuid.random(),
                created = Clock.System.now(),
                createdBy = "User",
                modified = Clock.System.now(),
                body = "Test Comment"
            )
        )

        every { mediaRepository.getComments(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Success(expectedComments)
        )

        // Act
        service.fetchCommentDetails(media)

        // Assert
        assertEquals(expectedComments, service.comments.value)
    }

    @Test
    fun `fetchCommentDetails sets empty list on failure`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )

        every { mediaRepository.getComments(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Error("Error")
        )

        // Act
        service.fetchCommentDetails(media)

        // Assert
        assertEquals(emptyList<Comment>(), service.comments.value)
    }

    @Test
    fun `addComment updates list on success`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )
        val commentText = "New Comment"
        val newComment = Comment(
            commentId = Uuid.random(),
            created = Clock.System.now(),
            createdBy = "User",
            modified = Clock.System.now(),
            body = commentText
        )

        every { mediaRepository.addComment(mediaId, commentText) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Success(newComment)
        )

        // Act
        service.addComment(media, commentText)

        // Assert
        assertEquals(listOf(newComment), service.comments.value)
        verify { mediaRepository.addComment(mediaId, commentText) }
    }

    @Test
    fun `addComment does nothing if text is blank`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(
            id = mediaId,
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false
        )

        // Act
        service.addComment(media, "  ")

        // Assert
        assertEquals(emptyList<Comment>(), service.comments.value)
        verify(exactly = 0) { mediaRepository.addComment(any(), any()) }
    }
}
