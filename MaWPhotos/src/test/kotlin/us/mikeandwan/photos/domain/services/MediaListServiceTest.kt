package us.mikeandwan.photos.domain.services

import android.graphics.drawable.Drawable
import android.webkit.MimeTypeMap
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonElement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.ScopeAccess
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaFaceRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.DetectedFace
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.MediaType

@OptIn(ExperimentalCoroutinesApi::class)
class MediaListServiceTest {

    private val mediaPreference = MutableStateFlow(MediaPreference())
    private val faceRecognitionAccess = MutableStateFlow(ScopeAccess.Granted)

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var randomMediaRepository: RandomMediaRepository
    private lateinit var faceFeedRepository: FaceFeedRepository
    private lateinit var fileRepository: FileStorageRepository
    private lateinit var mediaFavoriteService: MediaFavoriteService
    private lateinit var mediaCommentService: MediaCommentService
    private lateinit var mediaExifService: MediaExifService
    private lateinit var mediaFaceRepository: MediaFaceRepository
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var mediaFaceService: MediaFaceService
    private lateinit var mediaPreferenceRepository: MediaPreferenceRepository
    private lateinit var authService: AuthService
    private lateinit var service: MediaListService

    @Before
    fun setUp() {
        // the service does its work on the main dispatcher, and unconfined keeps that work in step
        // with the test rather than needing every assertion to advance a scheduler first
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // FileStorageRepository reads the mime type map while its class initialises, which is a
        // real android call rather than an instance the mock can stand in for
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns mockk(relaxed = true)

        categoryRepository = mockk(relaxed = true)
        randomMediaRepository = mockk(relaxed = true)
        faceFeedRepository = mockk(relaxed = true)
        fileRepository = mockk()
        mediaFavoriteService = mockk(relaxed = true)
        mediaCommentService = mockk(relaxed = true)
        mediaExifService = mockk(relaxed = true)
        mediaFaceRepository = mockk(relaxed = true)
        peopleRepository = mockk(relaxed = true)

        every { peopleRepository.people } returns MutableStateFlow(emptyList())

        mediaFaceService = MediaFaceService(mediaFaceRepository, peopleRepository)
        mediaPreferenceRepository = mockk(relaxed = true)
        authService = mockk(relaxed = true)

        every { authService.faceRecognitionAccess } returns faceRecognitionAccess

        // relaxed mocks hand back flows that never emit, and the state here is a combine - one
        // silent source is enough to keep it at its initial value forever
        every { mediaExifService.exif } returns MutableStateFlow<JsonElement?>(null)
        every { mediaCommentService.comments } returns MutableStateFlow<List<Comment>>(emptyList())
        every { mediaPreferenceRepository.getMediaPreference() } returns mediaPreference

        service = MediaListService(
            categoryRepository,
            randomMediaRepository,
            faceFeedRepository,
            fileRepository,
            mediaFavoriteService,
            mediaCommentService,
            mediaExifService,
            mediaFaceService,
            mediaPreferenceRepository,
            authService,
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
    }

    // leaving the pager resets it, and the same view model is reused for the next visit - so the
    // list has to survive that reset.  the feed it mirrors only re-emits when it changes, and it
    // has not, which would leave the second visit with nothing to show but a spinner.
    @Test
    fun `the media survives a reset so a second visit to the pager still has it`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val mediaList = listOf(
            Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false),
        )

        service.initialize(MutableStateFlow(mediaList), MutableStateFlow(5000L))
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Act - the pager is left, and then opened again over the same feed
        service.onAction(MediaListAction.Reset)
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        assertEquals(mediaList, service.state.value.media)
        assertEquals(mediaId, service.state.value.activeId)
        assertEquals(mediaList.first(), service.state.value.activeMedia)
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

    @Test
    fun `faces are fetched for the active photo while highlighting is on`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val face = DetectedFace(Uuid.random(), Uuid.random(), 0.1f, 0.1f, 0.2f, 0.2f)
        val media = Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false)

        mediaPreference.value = MediaPreference(showFaceHighlights = true)
        every { mediaFaceRepository.getFaces(mediaId) } returns
            flowOf(ExternalCallStatus.Success(listOf(face)))

        service.initialize(MutableStateFlow(listOf(media)), MutableStateFlow(5000L))

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        assertEquals(listOf(face.id), service.state.value.faces.map { it.id })
    }

    @Test
    fun `faces are not asked for while highlighting is off`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false)

        mediaPreference.value = MediaPreference(showFaceHighlights = false)

        service.initialize(MutableStateFlow(listOf(media)), MutableStateFlow(5000L))

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        verify(exactly = 0) { mediaFaceRepository.getFaces(any()) }
        assertTrue(service.state.value.faces.isEmpty())
    }

    // a box fixed to a frame would be wrong the moment the video moved, so a video is skipped
    // rather than asked about
    @Test
    fun `faces are not asked for on a video`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Video, isFavorite = false)

        mediaPreference.value = MediaPreference(showFaceHighlights = true)

        service.initialize(MutableStateFlow(listOf(media)), MutableStateFlow(5000L))

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        verify(exactly = 0) { mediaFaceRepository.getFaces(any()) }
        assertTrue(service.state.value.faces.isEmpty())
    }

    @Test
    fun `switching highlighting off drops the faces already drawn`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val faces = listOf(DetectedFace(Uuid.random(), null, 0.1f, 0.1f, 0.2f, 0.2f))
        val media = Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false)

        mediaPreference.value = MediaPreference(showFaceHighlights = true)
        every { mediaFaceRepository.getFaces(mediaId) } returns flowOf(ExternalCallStatus.Success(faces))

        service.initialize(MutableStateFlow(listOf(media)), MutableStateFlow(5000L))
        service.onAction(MediaListAction.SetActiveId(mediaId))
        assertTrue(service.state.value.faces.isNotEmpty())

        // Act
        mediaPreference.value = MediaPreference(showFaceHighlights = false)

        // Assert
        assertTrue(service.state.value.faces.isEmpty())
    }

    @Test
    fun `ToggleFaceHighlights action writes the preference`() = runTest {
        // Arrange
        mediaPreference.value = MediaPreference(showFaceHighlights = false)

        // Act
        service.onAction(MediaListAction.ToggleFaceHighlights)

        // Assert
        coVerify { mediaPreferenceRepository.setShowFaceHighlights(true) }
    }

    @Test
    fun `ToggleFaceHighlights action turns highlighting back off`() = runTest {
        // Arrange
        mediaPreference.value = MediaPreference(showFaceHighlights = true)

        // Act
        service.onAction(MediaListAction.ToggleFaceHighlights)

        // Assert
        coVerify { mediaPreferenceRepository.setShowFaceHighlights(false) }
    }

    // the pager reads this to decide whether to offer the switch at all
    @Test
    fun `highlighting is unavailable when the api would refuse the calls behind it`() = runTest {
        // Arrange
        faceRecognitionAccess.value = ScopeAccess.Granted
        assertTrue(service.state.value.canHighlightFaces)

        // Act
        faceRecognitionAccess.value = ScopeAccess.Denied

        // Assert
        assertFalse(service.state.value.canHighlightFaces)
    }

    // unknown is "nothing has been read yet" rather than a refusal, so it must not withdraw the
    // switch - see ScopeAccess
    @Test
    fun `highlighting stays on offer while access is still unknown`() = runTest {
        // Act
        faceRecognitionAccess.value = ScopeAccess.Unknown

        // Assert
        assertTrue(service.state.value.canHighlightFaces)
    }

    // the category names the screen and nothing more.  a lookup that fails or never answers used to
    // hold a spinner over media that was fully in hand.
    @Test
    fun `the pager is ready before the category that titles it arrives`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val mediaList = listOf(
            Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false),
        )

        every { categoryRepository.getCategory(any()) } returns emptyFlow()

        service.initialize(MutableStateFlow(mediaList), MutableStateFlow(5000L))

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        assertNull(service.state.value.category)
        assertFalse(service.state.value.isLoading)
    }

    // the state changes for reasons that have nothing to do with the item - faces arriving, the
    // detail sheet, a preference - and the category lookup ends in a room flow that never completes,
    // so one lookup per change would pile up collectors that each outlive the screen
    @Test
    fun `the category is looked up once however often the state changes`() = runTest {
        // Arrange
        val categoryId = Uuid.random()
        val mediaId = Uuid.random()
        val mediaList = listOf(
            Media(id = mediaId, categoryId = categoryId, type = MediaType.Photo, isFavorite = false),
        )

        every { categoryRepository.getCategory(categoryId) } returns emptyFlow()

        service.initialize(MutableStateFlow(mediaList), MutableStateFlow(5000L))
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Act - changes that have nothing to do with which item is showing
        service.onAction(MediaListAction.ToggleShowDetails)
        service.onAction(MediaListAction.ToggleShowDetails)
        mediaPreference.value = MediaPreference(showFaceHighlights = true)
        mediaPreference.value = MediaPreference(showFaceHighlights = false)

        // Assert
        verify(exactly = 1) { categoryRepository.getCategory(categoryId) }
    }

    // leaving and coming back has to ask again - the lookup was cancelled on the way out, so
    // remembering it as already requested would leave the screen with no title at all
    @Test
    fun `the category is looked up again after a reset`() = runTest {
        // Arrange
        val categoryId = Uuid.random()
        val mediaId = Uuid.random()
        val mediaList = listOf(
            Media(id = mediaId, categoryId = categoryId, type = MediaType.Photo, isFavorite = false),
        )

        every { categoryRepository.getCategory(categoryId) } returns emptyFlow()

        service.initialize(MutableStateFlow(mediaList), MutableStateFlow(5000L))
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Act
        service.onAction(MediaListAction.Reset)
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        verify(exactly = 2) { categoryRepository.getCategory(categoryId) }
    }

    // a view model can be reused for a second feed, and wiring one up twice would leave two
    // collectors writing the same state
    @Test
    fun `initializing again points at the new feed rather than both`() = runTest {
        // Arrange
        val first = listOf(
            Media(id = Uuid.random(), categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false),
        )
        val secondId = Uuid.random()
        val second = listOf(
            Media(id = secondId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false),
        )
        val firstSource = MutableStateFlow(first)

        service.initialize(firstSource, MutableStateFlow(5000L))

        // Act
        service.initialize(MutableStateFlow(second), MutableStateFlow(5000L))
        firstSource.value = emptyList()

        // Assert - the feed it was re-pointed at, undisturbed by the one it left
        assertEquals(second, service.state.value.media)

        service.onAction(MediaListAction.SetActiveId(secondId))
        assertEquals(secondId, service.state.value.activeId)
    }

    // the flows it collects outlive any one screen, so without this the service - and its state -
    // is held for the rest of the session, once per pager ever opened
    @Test
    fun `close stops the service following the feed`() = runTest {
        // Arrange
        val mediaList = listOf(
            Media(id = Uuid.random(), categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false),
        )
        val source = MutableStateFlow(mediaList)

        service.initialize(source, MutableStateFlow(5000L))
        assertEquals(mediaList, service.state.value.media)

        // Act
        service.close()
        source.value = emptyList()

        // Assert - no longer listening
        assertEquals(mediaList, service.state.value.media)
    }

    // credentials that lost the scope would otherwise keep asking for faces the API refuses, once
    // per photo, for a preference the user can no longer see
    @Test
    fun `faces are not asked for when the scope has been withdrawn`() = runTest {
        // Arrange
        val mediaId = Uuid.random()
        val media = Media(id = mediaId, categoryId = Uuid.random(), type = MediaType.Photo, isFavorite = false)

        mediaPreference.value = MediaPreference(showFaceHighlights = true)
        faceRecognitionAccess.value = ScopeAccess.Denied

        service.initialize(MutableStateFlow(listOf(media)), MutableStateFlow(5000L))

        // Act
        service.onAction(MediaListAction.SetActiveId(mediaId))

        // Assert
        verify(exactly = 0) { mediaFaceRepository.getFaces(any()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
}
