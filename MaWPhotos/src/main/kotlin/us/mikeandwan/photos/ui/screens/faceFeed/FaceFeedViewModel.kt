package us.mikeandwan.photos.ui.screens.faceFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.flowext.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.FaceFeedFilter
import us.mikeandwan.photos.domain.models.FaceFeedSubject
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.shared.toMediaGridItem

data class FaceFeedUiState(
    // the person's or clan's name, which is what the top bar is titled with.  empty until the list
    // it comes from has been read, which is why the route waits for it rather than titling the
    // screen with a placeholder.
    val title: String = "",
    val gridItems: List<MediaGridItem<Media>> = emptyList(),
    val thumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showFavoriteIndicator: Boolean = true,
    val favoritesOnly: Boolean = false,
    val isShuffled: Boolean = false,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    // told apart from loading so the screen can say there is nothing here rather than spin forever
    val isEmpty: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FaceFeedViewModel
    @Inject
    constructor(
        private val faceFeedRepository: FaceFeedRepository,
        private val peopleRepository: PeopleRepository,
        private val clanRepository: ClanRepository,
        mediaPreferenceRepository: MediaPreferenceRepository,
        private val mediaFavoriteService: MediaFavoriteService,
    ) : ViewModel() {
        private val _subject = MutableStateFlow<FaceFeedSubject?>(null)
        private val _isLoading = MutableStateFlow(true)

        private val _uiState = MutableStateFlow(FaceFeedUiState())
        val uiState = _uiState.asStateFlow()

        // read from the list the subject lives in rather than fetched on its own: both lists are
        // already held whole, and taking the name from there means a rename or a favorite toggle
        // made elsewhere is reflected here without asking again
        private val title = _subject
            .flatMapLatest { subject ->
                when (subject) {
                    null -> flowOf("")

                    is FaceFeedSubject.Person -> peopleRepository.people.map { people ->
                        people.firstOrNull { it.id == subject.personId }?.name ?: ""
                    }

                    is FaceFeedSubject.Clan -> clanRepository.clans.map { clans ->
                        clans.firstOrNull { it.id == subject.clanId }?.name ?: ""
                    }
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), "")

        init {
            val thumbnailSizeFlow = mediaPreferenceRepository
                .getPhotoGridItemSize()
                .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

            // flowext's combine rather than the one in kotlinx: the typed overloads there stop at
            // five flows, and this state has more parts than that
            combine(
                faceFeedRepository.media,
                faceFeedRepository.hasMore,
                faceFeedRepository.filter,
                title,
                thumbnailSizeFlow,
                mediaPreferenceRepository.getMediaPreference(),
                _isLoading,
            ) { media, hasMore, filter, title, thumbnailSize, mediaPref, isLoading ->
                FaceFeedUiState(
                    title = title,
                    gridItems = media.map {
                        it.toMediaGridItem(
                            useLargeTeaser = thumbnailSize == GridThumbnailSize.Large,
                            showMediaTypeIndicator = mediaPref.showMediaTypeIndicator,
                        )
                    },
                    thumbnailSize = thumbnailSize,
                    showFavoriteIndicator = mediaPref.showFavoriteIndicator,
                    favoritesOnly = filter.favoritesOnly,
                    isShuffled = filter.seed != null,
                    hasMore = hasMore,
                    isLoading = isLoading && media.isEmpty(),
                    isEmpty = !isLoading && media.isEmpty(),
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

        fun initState(subject: FaceFeedSubject) {
            if (_subject.value == subject) {
                return
            }

            _subject.update { subject }
            faceFeedRepository.initialize(subject)

            ensureSubjectIsNamed(subject)
            loadNextPage()
        }

        fun loadNextPage() {
            viewModelScope.launch {
                faceFeedRepository
                    .loadNextPage()
                    .collect { status ->
                        if (status !is ExternalCallStatus.Loading) {
                            _isLoading.update { false }
                        }
                    }

                // a call the repository declined - already loading, or nothing more to fetch -
                // emits nothing at all, which must still clear the initial loading state
                _isLoading.update { false }
            }
        }

        fun setFavoritesOnly(favoritesOnly: Boolean) {
            applyFilter { it.copy(favoritesOnly = favoritesOnly) }
        }

        /**
         * A seed rather than a flag: the feed is paged, and the API orders by a hash of the media id
         * and this seed, so one seed held for the life of the shuffle is what keeps page two from
         * repeating and skipping rows from page one.
         */
        fun setShuffled(isShuffled: Boolean) {
            applyFilter { it.copy(seed = if (isShuffled) Random.nextLong() else null) }
        }

        fun toggleFavorite(media: Media) {
            viewModelScope.launch {
                val isFavorite = mediaFavoriteService.setIsFavorite(media, !media.isFavorite)

                faceFeedRepository.updateMedia(media.copy(isFavorite = isFavorite))
            }
        }

        private fun applyFilter(update: (FaceFeedFilter) -> FaceFeedFilter) {
            val next = update(faceFeedRepository.filter.value)

            if (next == faceFeedRepository.filter.value) {
                return
            }

            _isLoading.update { true }
            faceFeedRepository.setFilter(next)

            loadNextPage()
        }

        // the name comes from a list this screen may have been opened without: a cold start
        // restoring straight into a feed has neither list in hand yet
        private fun ensureSubjectIsNamed(subject: FaceFeedSubject) {
            viewModelScope.launch {
                when (subject) {
                    is FaceFeedSubject.Person -> peopleRepository.getPeople().collect { }
                    is FaceFeedSubject.Clan -> clanRepository.getClans().collect { }
                }
            }
        }
    }
