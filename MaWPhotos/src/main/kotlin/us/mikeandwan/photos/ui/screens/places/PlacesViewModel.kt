package us.mikeandwan.photos.ui.screens.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Place
import us.mikeandwan.photos.domain.models.PlaceAncestor

data class PlacesUiState(
    // where in the tree the screen is, null at the root.  carried separately from [place] because
    // it is known the moment the screen opens, while the place it names arrives later.
    val placeId: Uuid? = null,
    val place: Place? = null,
    // the chain above and including [place], country first.  empty at the root, where there is no
    // path to name.
    val chain: List<PlaceAncestor> = emptyList(),
    // the places inside this one
    val places: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    // a place that answered 404: a stale link, or one a merge has since folded away.  told apart
    // from an empty listing, which reads as a place with nothing inside it.
    val isPlaceMissing: Boolean = false,
) {
    /**
     * Whether the listing is worth drawing at all.
     *
     * At the bottom of the tree it is left out rather than shown empty: its tiles are the way
     * further down and there is no further down, and the photographs are offered above it instead.
     */
    val showsChildren: Boolean
        get() = when {
            placeId == null || places.isNotEmpty() -> true

            // the place itself lands before its children do and already knows: without this a city
            // would flash a grid of skeletons on the way to showing nothing
            place != null -> !place.isLeaf

            else -> isLoading
        }
}

/**
 * Browsing by where a photograph was taken.
 *
 * A drill-down rather than a list: countries, then their states, then their cities, with the
 * photographs of a whole subtree one tap away at every level. Every listing is scoped by the API to
 * what the caller can see, so a place holding nothing visible to them is absent rather than empty -
 * which is the whole read access story here, and why nothing on this screen checks a permission.
 */
@HiltViewModel
class PlacesViewModel
    @Inject
    constructor(
        private val placeRepository: PlaceRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PlacesUiState())
        val uiState = _uiState.asStateFlow()

        // every place read so far, which is where the breadcrumb finds its covers: drilling down
        // populated it on the way through, so walking back up costs nothing
        val knownPlaces = placeRepository.placesById

        // told apart from "the id is null" so the root can be pointed at once rather than on every
        // recomposition that hands the same null back
        private var isPointed = false

        // the reads in flight, so a level left before it answered cannot write over the one that
        // replaced it
        private var listingJob: Job? = null
        private var placeJob: Job? = null

        /** Points the screen at a level of the tree, the root when [placeId] is null. */
        fun setPlace(placeId: Uuid?) {
            if (isPointed && _uiState.value.placeId == placeId) {
                return
            }

            isPointed = true

            _uiState.update { PlacesUiState(placeId = placeId, isLoading = true) }

            loadPlace(placeId)
            loadListing(placeId)
        }

        private fun loadListing(placeId: Uuid?) {
            listingJob?.cancel()
            listingJob = viewModelScope.launch {
                placeRepository.getPlaces(placeId).collect { status ->
                    when (status) {
                        is ExternalCallStatus.Loading -> {}

                        // already reported the way every other failed call is; the screen only has
                        // to stop waiting
                        is ExternalCallStatus.Error -> {
                            _uiState.update { it.copy(isLoading = false) }
                        }

                        is ExternalCallStatus.Success -> {
                            _uiState.update { it.copy(places = status.result, isLoading = false) }
                        }
                    }
                }
            }
        }

        /**
         * The place itself and the chain above it, which between them say where the screen is and
         * how much is here.
         *
         * Both are left to arrive on their own: the listing is what the screen is for, and holding
         * it back for a breadcrumb would be the wrong trade.
         */
        private fun loadPlace(placeId: Uuid?) {
            placeJob?.cancel()

            if (placeId == null) {
                return
            }

            placeJob = viewModelScope.launch {
                launch {
                    placeRepository.getPlace(placeId).collect { status ->
                        when (status) {
                            is ExternalCallStatus.Loading -> {}

                            is ExternalCallStatus.Error -> {
                                _uiState.update { it.copy(isPlaceMissing = true) }
                            }

                            is ExternalCallStatus.Success -> {
                                _uiState.update {
                                    it.copy(place = status.result, isPlaceMissing = false)
                                }
                            }
                        }
                    }
                }

                launch {
                    placeRepository.getAncestors(placeId).collect { status ->
                        if (status is ExternalCallStatus.Success) {
                            _uiState.update { it.copy(chain = status.result) }
                        }
                    }
                }
            }
        }
    }
