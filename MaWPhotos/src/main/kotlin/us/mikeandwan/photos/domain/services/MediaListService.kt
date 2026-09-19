package us.mikeandwan.photos.domain.services

import com.hoc081098.flowext.combine
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.ScopeAccess
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeriodicJob
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.FaceHighlight
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFaces
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.domain.models.Place

// paired so the pager can tell "off" from "not on offer" without two flows threaded through
private data class FaceHighlighting(
    val isOn: Boolean,
    val isAvailable: Boolean,
)

// the item whose faces are wanted, and whether the overlay is one of the things that wants them.
// the two travel together so that moving to another item always drops the previous item's faces,
// while merely switching the overlay off does not - the details sheet's Who card reads the same
// faces, and has no reason to forget them because the boxes were hidden.
private data class FaceTarget(
    val mediaId: Uuid?,
    val wantedByOverlay: Boolean,
)

sealed class MediaListAction {
    data object Reset : MediaListAction()

    data class SetActiveId(
        val id: Uuid,
    ) : MediaListAction()

    data object ToggleSlideshow : MediaListAction()

    data object ToggleShowDetails : MediaListAction()

    data class SetIsZoomed(
        val isZoomed: Boolean,
    ) : MediaListAction()

    data class SetIsFavorite(
        val isFavorite: Boolean,
    ) : MediaListAction()

    data object ToggleFaceHighlights : MediaListAction()

    data object FetchExif : MediaListAction()

    data object FetchComments : MediaListAction()

    /**
     * Asks for the faces in the active item on behalf of the details sheet's Who card.
     *
     * Separate from the overlay's own fetching, which only ever runs while face highlighting is
     * switched on: the card is worth reading whether or not somebody wants boxes drawn over their
     * photographs.
     */
    data object FetchFaces : MediaListAction()

    data object FetchPlaces : MediaListAction()

    data class AddComment(
        val comment: String,
    ) : MediaListAction()

    data class SaveFileToShare(
        val url: String,
        val onComplete: (File) -> Unit,
    ) : MediaListAction()
}

data class MediaListState(
    val category: Category? = null,
    val media: List<Media> = emptyList(),
    val activeId: Uuid = Uuid.NIL,
    val isSlideshowPlaying: Boolean = false,
    val showDetailSheet: Boolean = false,
    val exif: JsonElement? = null,
    val comments: List<Comment> = emptyList(),
    // what the recognition pipeline found in the active item, whether or not anybody asked for
    // boxes over it - the overlay and the details sheet's Who card are both drawn from this.  null
    // until something has asked, which is not the same as having found nobody.
    val mediaFaces: MediaFaces? = null,
    // where the active item was taken.  null until the details sheet's Where card asks, so nothing
    // is fetched for a tab nobody opened.
    val places: List<Place>? = null,
    val showFaceHighlights: Boolean = false,
    // false when the API would refuse the calls behind the overlay, which is what keeps the pager
    // from offering a switch that could only ever turn on an empty overlay
    val canHighlightFaces: Boolean = false,
) {
    /**
     * The boxes to draw over the media, which is not the same as the faces that were found in it.
     *
     * Empty whenever face highlighting is switched off, so nothing downstream needs to know the
     * preference exists - there is simply nothing to draw. The Who card reads [mediaFaces]
     * instead, which is why opening it cannot put boxes over a photograph nobody asked to mark up.
     */
    val facesToHighlight: List<FaceHighlight>
        get() = if (showFaceHighlights && canHighlightFaces) {
            mediaFaces?.highlights.orEmpty()
        } else {
            emptyList()
        }

    val activeIndex: Int
        get() = media.indexOfFirst { it.id == activeId }

    val activeMedia: Media?
        get() = media.firstOrNull { it.id == activeId }

    // the category is not part of this: it names the screen, and the pager can draw the photo
    // perfectly well before its title arrives.  waiting on it turned any category that failed to
    // load into a permanent spinner over media that was already in hand.
    val isLoading: Boolean
        get() = media.isEmpty() || activeId == Uuid.NIL || activeMedia == null

    val hasPrevious: Boolean
        get() = activeIndex > 0

    val hasNext: Boolean
        get() = activeIndex >= 0 && activeIndex < media.size - 1
}

@ViewModelScoped
class MediaListService
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val randomMediaRepository: RandomMediaRepository,
        private val mediaFeedRepository: MediaFeedRepository,
        private val fileRepository: FileStorageRepository,
        private val mediaFavoriteService: MediaFavoriteService,
        private val mediaCommentService: MediaCommentService,
        private val mediaExifService: MediaExifService,
        private val mediaFaceService: MediaFaceService,
        private val mediaPlaceService: MediaPlaceService,
        private val mediaPreferenceRepository: MediaPreferenceRepository,
        private val errorRepository: ErrorRepository,
        authService: AuthService,
    ) {
        companion object {
            private const val ERR_MSG_SHARE = "Unable to share this photo at this time.  Please try again later."
        }

        private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        // the two halves of "can this be switched on, and is it" travel together so the state below
        // stays one flow shorter than it would with each of them separate
        private val faceHighlighting = combine(
            mediaPreferenceRepository.getMediaPreference(),
            authService.faceRecognitionAccess,
        ) { pref, access ->
            FaceHighlighting(
                isOn = pref.showFaceHighlights,
                isAvailable = access != ScopeAccess.Denied,
            )
        }
        private val category = MutableStateFlow<Category?>(null)
        private val media = MutableStateFlow<List<Media>>(emptyList())
        private val activeId = MutableStateFlow(Uuid.NIL)
        private val slideshowJob = PeriodicJob { moveNext() }
        private val isZoomed = MutableStateFlow(false)

        // whether the slideshow was running when something paused it.  both the detail sheet and
        // zooming in pause it, and it only picks up again once neither is in the way - closing the
        // sheet while still zoomed must not start moving the photo out from under somebody.
        private val resumeSlideshowWhenUnpaused = MutableStateFlow(false)
        private val showDetailSheet = MutableStateFlow(false)

        // everything [initialize] wires up, held as one job so pointing this at another feed
        // replaces that wiring instead of adding a second copy of it
        private var wiring: Job? = null

        // the category lookup never completes on its own - it ends in a room flow - so it is held
        // and cancelled rather than launched and forgotten, and the id it was asked for is
        // remembered so repeated asks for the same one do not each start another
        private var categoryJob: Job? = null
        private var requestedCategoryId: Uuid? = null

        val state: StateFlow<MediaListState> =
            combine(
                category,
                media,
                activeId,
                slideshowJob.isRunning,
                showDetailSheet,
                mediaExifService.exif,
                mediaCommentService.comments,
                mediaFaceService.faces,
                mediaPlaceService.places,
                faceHighlighting,
            ) {
                category,
                media,
                activeId,
                isSlideshowPlaying,
                showDetailSheet,
                exif,
                comments,
                mediaFaces,
                places,
                faceHighlighting,
                ->
                MediaListState(
                    category = category,
                    media = media,
                    activeId = activeId,
                    isSlideshowPlaying = isSlideshowPlaying,
                    showDetailSheet = showDetailSheet,
                    exif = exif,
                    comments = comments,
                    mediaFaces = mediaFaces,
                    places = places,
                    showFaceHighlights = faceHighlighting.isOn,
                    canHighlightFaces = faceHighlighting.isAvailable,
                )
            }.stateIn(scope, SharingStarted.Eagerly, MediaListState())

        fun onAction(action: MediaListAction) {
            when (action) {
                is MediaListAction.Reset -> {
                    reset()
                }

                is MediaListAction.SetActiveId -> {
                    setActiveId(action.id)
                }

                is MediaListAction.ToggleSlideshow -> {
                    toggleSlideshow()
                }

                is MediaListAction.ToggleShowDetails -> {
                    toggleShowDetails()
                }

                is MediaListAction.SetIsZoomed -> {
                    setIsZoomed(action.isZoomed)
                }

                is MediaListAction.SetIsFavorite -> {
                    setIsFavorite(action.isFavorite)
                }

                is MediaListAction.ToggleFaceHighlights -> {
                    toggleFaceHighlights()
                }

                is MediaListAction.FetchExif -> {
                    fetchExif()
                }

                is MediaListAction.FetchComments -> {
                    fetchComments()
                }

                is MediaListAction.FetchFaces -> {
                    fetchFaces()
                }

                is MediaListAction.FetchPlaces -> {
                    fetchPlaces()
                }

                is MediaListAction.AddComment -> {
                    addComment(action.comment)
                }

                is MediaListAction.SaveFileToShare -> {
                    saveFileToShare(action.url, action.onComplete)
                }
            }
        }

        private fun reset() {
            slideshowJob.stop()
            cancelCategoryLoad()
            // the list is deliberately left alone.  it is a mirror of the feed the pager was opened
            // over, kept by a collector that outlives this screen, and that feed only re-emits when
            // it actually changes - so emptying it here leaves nothing to refill it, and the next
            // visit to the pager waits forever for media it already had.
            activeId.update { Uuid.NIL }
            showDetailSheet.update { false }
            isZoomed.update { false }
            resumeSlideshowWhenUnpaused.update { false }
            mediaFaceService.clear()
            mediaPlaceService.clear()
        }

        /**
         * Releases everything this holds, for a view model that is going away.
         *
         * The scope below is this object's own, and the flows it collects - the feed, the
         * preferences, the granted scopes - all outlive any one screen.  They would hold this and
         * its state alive for the rest of the session otherwise, once per pager ever opened.
         */
        fun close() {
            slideshowJob.cancel()
            scope.cancel()
        }

        private fun setActiveId(id: Uuid) {
            activeId.update { id }
        }

        private fun toggleSlideshow() {
            // an explicit play or stop settles it - nothing paused is waiting to be resumed any more
            resumeSlideshowWhenUnpaused.update { false }

            if (slideshowJob.isRunning.value) {
                stopSlideshow()
            } else {
                startSlideshow()
            }
        }

        private fun startSlideshow() {
            slideshowJob.start()
        }

        private fun stopSlideshow() {
            slideshowJob.stop()
        }

        private fun toggleShowDetails() {
            if (showDetailSheet.value) {
                showDetailSheet.update { false }
                resumeSlideshowIfUnpaused()
            } else {
                pauseSlideshow()
                showDetailSheet.update { true }
            }
        }

        // a slideshow that keeps going while somebody is zoomed in on a detail would carry the
        // photo away mid-look
        private fun setIsZoomed(zoomed: Boolean) {
            if (zoomed == isZoomed.value) {
                return
            }

            isZoomed.update { zoomed }

            if (zoomed) {
                pauseSlideshow()
            } else {
                resumeSlideshowIfUnpaused()
            }
        }

        private fun pauseSlideshow() {
            if (slideshowJob.isRunning.value) {
                resumeSlideshowWhenUnpaused.update { true }
                slideshowJob.stop()
            }
        }

        private fun resumeSlideshowIfUnpaused() {
            if (resumeSlideshowWhenUnpaused.value && !showDetailSheet.value && !isZoomed.value) {
                resumeSlideshowWhenUnpaused.update { false }
            slideshowJob.start()
        }
        }

        private fun saveFileToShare(
            url: String,
            onComplete: (File) -> Unit,
        ) {
            scope.launch {
                val file = try {
                    fileRepository.saveMediaToShare(url)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorRepository.logError("Unable to prepare $url to share", e)
                    errorRepository.showError(ERR_MSG_SHARE)
                    return@launch
                }

                onComplete(file)
            }
        }

        // FAVORITES
        private fun setIsFavorite(isFavorite: Boolean) {
            scope.launch {
                val currentMedia = state.value.activeMedia ?: return@launch

                val resultIsFav = mediaFavoriteService.setIsFavorite(currentMedia, isFavorite)

                // Update the media list safely
                media.update { currentMediaList ->
                    val updatedMedia = currentMediaList.toMutableList()
                    val currentIndex = updatedMedia.indexOfFirst { it.id == currentMedia.id }

                    if (
                        currentIndex >= 0 &&
                        currentIndex < updatedMedia.size &&
                        updatedMedia[currentIndex].id == currentMedia.id
                    ) {
                        val updatedItem = currentMedia.copy(isFavorite = resultIsFav)
                        updatedMedia[currentIndex] = updatedItem

                        // keep the lists backing the grids in sync so the favorite badge there
                        // reflects what was just toggled in the pager
                        categoryRepository.tryUpdateCache(updatedItem)
                        randomMediaRepository.updateMedia(updatedItem)
                        mediaFeedRepository.updateMedia(updatedItem)

                        updatedMedia
                    } else {
                        currentMediaList
                    }
                }
            }
        }

        // FACES
        // written to the preference rather than held for this screen: somebody who turns the boxes
        // on here means it for the grid they came from and the next photo they open, which is the
        // same setting the settings screen offers
        private fun toggleFaceHighlights() {
            scope.launch {
                mediaPreferenceRepository.setShowFaceHighlights(!state.value.showFaceHighlights)
            }
        }

        // EXIF
        private fun fetchExif() {
            scope.launch {
                state.value.activeMedia?.let { mediaExifService.fetchExifDetails(it) }
            }
        }

        // COMMENTS
        private fun fetchComments() {
            scope.launch {
                state.value.activeMedia?.let { mediaCommentService.fetchCommentDetails(it) }
            }
        }

        private fun addComment(comment: String) {
            scope.launch {
                state.value.activeMedia?.let { mediaCommentService.addComment(it, comment) }
            }
        }

        // FACES, ON BEHALF OF THE DETAILS SHEET
        // photos only, the same as the overlay's own fetching: faces are detected on stills, and a
        // card over a video would be asking a question the pipeline never answered
        private fun fetchFaces() {
            scope.launch {
                state.value.activeMedia
                    ?.takeIf { it.type == MediaType.Photo }
                    ?.let { mediaFaceService.fetchFaces(it.id) }
            }
        }

        // PLACES
        private fun fetchPlaces() {
            scope.launch {
                state.value.activeMedia?.let { mediaPlaceService.fetchPlaces(it.id) }
            }
        }

        /**
         * Points this at the feed a pager is being opened over.
         *
         * Safe to call again: everything it starts is replaced rather than added to, so a view
         * model that is reused for a second feed does not end up with two collectors writing the
         * same state.
         */
        fun initialize(
            sourceMedia: StateFlow<List<Media>>,
            slideshowDurationInMillis: StateFlow<Long>,
        ) {
            wiring?.cancel()
            cancelCategoryLoad()

            wiring = scope.launch {
                // the list is a mirror of the feed, seeded from what it already holds - a state flow
                // hands its current value to a new collector, which is what makes a second visit to
                // the pager find the media it had rather than an empty list
                launch {
                    sourceMedia.collect { newList -> media.update { newList } }
                }

                // the title follows the item, which in a random or media feed means it changes as the
                // pager moves.  loadCategory is what keeps repeated asks for the same category from
                // each starting their own lookup - this fires on every state change, not just on a
                // new item.
                launch {
                    state
                        .mapNotNull { it.activeMedia?.categoryId }
                        .collect { categoryId -> loadCategory(categoryId) }
                }

                launch {
                    slideshowDurationInMillis.collect { slideshowJob.setIntervalMillis(it) }
                }

                launch { watchFacesForActiveMedia() }
            }
        }

        /**
         * Keeps the face overlay pointed at whatever is on screen, and only while the preference
         * asks for it - which is what makes the whole feature cost nothing at all for anyone who
         * leaves it switched off.
         *
         * Videos are skipped rather than asked about: the overlay is drawn over a still frame, and
         * a box fixed to the frame would be wrong the moment the video moved.
         */
        private suspend fun watchFacesForActiveMedia() {
            var lastMediaId: Uuid? = null

            combine(
                media,
                activeId,
                faceHighlighting,
            ) { mediaList, id, highlighting ->
                FaceTarget(
                    mediaId = mediaList.firstOrNull { it.id == id && it.type == MediaType.Photo }?.id,
                    // availability is read here and not only where the switch is drawn: credentials
                    // that lost the scope would otherwise keep asking for faces the API refuses,
                    // once per photo, on behalf of a preference the user can no longer see
                    wantedByOverlay = highlighting.isOn && highlighting.isAvailable,
                )
            }.distinctUntilChanged()
                // faces belong to the item they were fetched for, so they go the instant the item
                // does rather than lingering over the next one while it loads.  hiding the boxes is
                // not the same event, and does not throw away what the Who card is reading.
                .onEach { target ->
                    if (target.mediaId != lastMediaId) {
                        lastMediaId = target.mediaId
                        mediaFaceService.clear()
                    }
                }.collectLatest { target ->
                    if (target.wantedByOverlay && target.mediaId != null) {
                        mediaFaceService.fetchFaces(target.mediaId)
                    }
                }
        }

        private fun loadCategory(categoryId: Uuid) {
            if (requestedCategoryId == categoryId) {
                return
            }

            requestedCategoryId = categoryId
            categoryJob?.cancel()
            category.update { null }

            categoryJob = scope.launch {
                categoryRepository
                    .getCategory(categoryId)
                    .collect { newCategory -> category.update { newCategory } }
            }
        }

        private fun cancelCategoryLoad() {
            categoryJob?.cancel()
            categoryJob = null
            // forgotten as well as cancelled, so the next visit asks again rather than assuming the
            // category it was showing before is still loaded
            requestedCategoryId = null
            category.update { null }
        }

        private fun moveNext() =
            flow<Unit> {
                val activeIndex = media.value.indexOfFirst { it.id == activeId.value }
                val nextIndex = activeIndex + 1

                if (nextIndex < media.value.size) {
                    setActiveId(media.value[nextIndex].id)
                } else {
                    stopSlideshow()
                }
            }
    }
