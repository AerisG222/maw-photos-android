package us.mikeandwan.photos.domain.services

import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.MediaFaceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.DetectedFace
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Person

class MediaFaceServiceTest {

    private val people = MutableStateFlow<List<Person>>(emptyList())

    private lateinit var mediaFaceRepository: MediaFaceRepository
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var service: MediaFaceService

    @Before
    fun setUp() {
        mediaFaceRepository = mockk()
        peopleRepository = mockk(relaxed = true)

        every { peopleRepository.people } returns people
        every { peopleRepository.getPeople(any()) } returns flowOf(ExternalCallStatus.Success(emptyList()))

        service = MediaFaceService(mediaFaceRepository, peopleRepository)
    }

    @Test
    fun `fetchFaces updates faces on success`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val face = DetectedFace(Uuid.random(), Uuid.random(), 0.1f, 0.1f, 0.2f, 0.2f)

        every { mediaFaceRepository.getFaces(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Success(listOf(face)),
        )

        // Act
        service.fetchFaces(mediaId)

        // Assert
        val faces = service.faces.first()
        assertEquals(1, faces.size)
        assertEquals(face.id, faces.first().id)
        assertEquals(face.boxX, faces.first().boxX, 0.0001f)
    }

    // the boxes come from the media and the names from a list the app holds whole, so a face is
    // drawn as soon as it is known and picks its label up when that list has been read
    @Test
    fun `a face is labelled with the person it belongs to`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val personId = Uuid.random()
        val face = DetectedFace(Uuid.random(), personId, 0.1f, 0.1f, 0.2f, 0.2f)

        every { mediaFaceRepository.getFaces(mediaId) } returns
            flowOf(ExternalCallStatus.Success(listOf(face)))

        service.fetchFaces(mediaId)
        assertNull(service.faces.first().first().name)

        // Act - the people list is read after the faces were
        people.value = listOf(Person(personId, "Mike Morano", null, 12, false))

        // Assert
        assertEquals("Mike Morano", service.faces.first().first().name)
    }

    // unassigned, or a person this caller may not know about - the API makes those two
    // indistinguishable, and neither gets a label
    @Test
    fun `a face with nobody to name goes unlabelled`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val face = DetectedFace(Uuid.random(), null, 0.1f, 0.1f, 0.2f, 0.2f)

        people.value = listOf(Person(Uuid.random(), "Somebody Else", null, 3, false))
        every { mediaFaceRepository.getFaces(mediaId) } returns
            flowOf(ExternalCallStatus.Success(listOf(face)))

        // Act
        service.fetchFaces(mediaId)

        // Assert
        assertNull(service.faces.first().first().name)
        assertNull(service.faces.first().first().personId)
    }

    // the pager can be opened from a category, which never touches the people list - without this
    // every label would be missing until the user happened to visit the people screen
    @Test
    fun `fetching faces also makes sure the people list has been read`() = runTest {
        // Arrange
        val mediaId = Uuid.random()

        every { mediaFaceRepository.getFaces(mediaId) } returns
            flowOf(ExternalCallStatus.Success(emptyList()))

        // Act
        service.fetchFaces(mediaId)

        // Assert
        io.mockk.verify { peopleRepository.getPeople(any()) }
    }

    // a failure leaves nothing to draw rather than saying so - the boxes are an embellishment over
    // the media, and the failed call has already been reported the way every other one is
    @Test
    fun `fetchFaces leaves nothing to draw on failure`() = runTest {
        // Arrange
        val mediaId = Uuid.random()

        every { mediaFaceRepository.getFaces(mediaId) } returns flowOf(
            ExternalCallStatus.Loading,
            ExternalCallStatus.Error("Error"),
        )

        // Act
        service.fetchFaces(mediaId)

        // Assert
        assertTrue(service.faces.first().isEmpty())
    }

    @Test
    fun `clear drops the faces held for the previous item`() = runTest {
        // Arrange
        val mediaId = Uuid.random()

        every { mediaFaceRepository.getFaces(mediaId) } returns flowOf(
            ExternalCallStatus.Success(
                listOf(DetectedFace(Uuid.random(), null, 0.1f, 0.1f, 0.2f, 0.2f)),
            ),
        )

        service.fetchFaces(mediaId)
        assertTrue(service.faces.first().isNotEmpty())

        // Act
        service.clear()

        // Assert
        assertTrue(service.faces.first().isEmpty())
    }
}
