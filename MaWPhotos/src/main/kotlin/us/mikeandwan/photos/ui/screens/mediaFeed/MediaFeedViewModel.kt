package us.mikeandwan.photos.ui.screens.mediaFeed

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.MediaFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.PlacePreferenceRepository
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryLabels
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.PlaceAncestor
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.shared.toMediaGridItem

data class MediaFeedUiState(
    // the subject's name, which is what the top bar is titled with.  empty until the listing it
    // comes from has been read, which is why the route waits for it rather than titling the screen
    // with a placeholder.
    val title: String = "",
    // where in the place tree the subject sits, for the breadcrumb above the listing.  empty for a
    // person or a clan, who are not anywhere in it.
    val placeChain: List<PlaceAncestor> = emptyList(),
    val gridItems: List<MediaGridItem<Media>> = emptyList(),
    val thumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showFavoriteIndicator: Boolean = true,
    val favoritesOnly: Boolean = false,
    val isShuffled: Boolean = false,
    // whether the categories the subject turns up in are being listed rather than the media itself
    val showCategories: Boolean = false,
    val categories: List<Category> = emptyList(),
    // the categories are drawn the way the rest of the app draws categories, so whoever prefers a
    // list of them to a wall of teasers gets one here too
    val categoryPreference: CategoryPreference = CategoryPreference(),
    // what each of those categories says about itself beyond its teaser
    val categoryLabels: CategoryLabels = CategoryLabels(),
    // both of these describe whichever listing is on screen, so the screen does not have to ask
    // which one it is showing before it can page or say there is nothing here
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    // told apart from loading so the screen can say there is nothing here rather than spin forever
    val isEmpty: Boolean = false,
)

// what the screen says about the subject itself rather than about its media, folded together so the
// state above can still be built in one combine
private data class SubjectHeading(
    val title: String = "",
    val placeChain: List<PlaceAncestor> = emptyList(),
)

// the parts of the categories listing, folded together so the state above can be built in one
// combine rather than two
private data class CategoryListing(
    val categories: List<Category> = emptyList(),
    val hasMore: Boolean = false,
    val preference: CategoryPreference = CategoryPreference(),
    val labels: CategoryLabels = CategoryLabels(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaFeedViewModel
    @Inject
    constructor(
        private val mediaFeedRepository: MediaFeedRepository,
        private val peopleRepository: PeopleRepository,
        private val clanRepository: ClanRepository,
        private val placeRepository: PlaceRepository,
        private val categoryRepository: CategoryRepository,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
        private val placePreferenceRepository: PlacePreferenceRepository,
        categoryPreferenceRepository: CategoryPreferenceRepository,
        mediaPreferenceRepository: MediaPreferenceRepository,
        private val mediaFavoriteService: MediaFavoriteService,
    ) : ViewModel() {
        private val _subject = MutableStateFlow<MediaFeedSubject?>(null)
        private val _isLoading = MutableStateFlow(true)

        private val _uiState = MutableStateFlow(MediaFeedUiState())
        val uiState = _uiState.asStateFlow()

        // read from whatever the subject was listed in rather than fetched on its own: those
        // listings are already held, and taking the name from there means a rename or a favorite
        // toggle made elsewhere is reflected here without asking again
        private val title = _subject
            .flatMapLatest { subject ->
                when (subject) {
                    null -> flowOf("")

                    is MediaFeedSubject.Person -> peopleRepository.people.map { people ->
                        people.firstOrNull { it.id == subject.personId }?.name ?: ""
                    }

                    is MediaFeedSubject.Clan -> clanRepository.clans.map { clans ->
                        clans.firstOrNull { it.id == subject.clanId }?.name ?: ""
                    }

                    is MediaFeedSubject.Place -> placeRepository.placesById.map { places ->
                        places[subject.placeId]?.name ?: ""
                    }
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), "")

    /*
       The chain of places above and including the one being browsed.

       Drawn over the feed as well as over the browse because a place with nothing inside it
       opens its photographs directly - so for a city this is the only place the chain above it
       is ever shown.  Failures are simply never emitted: the strip is a convenience over a feed
       that is already on screen, and the browse says the same thing about a place that cannot
       be read.
     */
        private val placeChain = _subject
            .flatMapLatest { subject ->
                when (subject) {
                    is MediaFeedSubject.Place -> {
                        placeRepository
                            .getAncestors(subject.placeId)
                            .filterIsInstance<ExternalCallStatus.Success<List<PlaceAncestor>>>()
                            .map { it.result }
                    }

                    else -> {
                        flowOf(emptyList())
                    }
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    /*
       Whether the listed categories carry their year and their title.

       Read from whichever area the subject belongs to, and written back the same way - the two
       areas share this feed but are read differently, so a choice made while walking a country is
       not one made about a person.
     */
        private val categoryLabels = _subject
            .flatMapLatest { subject ->
                when (subject) {
                    is MediaFeedSubject.Place -> {
                        placePreferenceRepository
                            .getPlacePreference()
                            .map { CategoryLabels(it.showCategoryYear, it.showCategoryTitle) }
                    }

                    else -> {
                        peoplePreferenceRepository
                            .getPeoplePreference()
                            .map { CategoryLabels(it.showCategoryYear, it.showCategoryTitle) }
                    }
                }
            }

        // every place read so far, which is where the breadcrumb finds its covers.  drilling to a
        // place populated it on the way through; a cold start into a feed holds only the place
        // itself, and the strip draws an icon for the rest.
    val knownPlaces = placeRepository.placesById

    private val subjectHeading = combine(title, placeChain) { title, chain ->
        SubjectHeading(title, chain)
    }

        init {
            val thumbnailSizeFlow = mediaPreferenceRepository
                .getPhotoGridItemSize()
                .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

            // kotlinx's combine here - flowext's overloads start at six flows
            val categoryListing = combine(
                mediaFeedRepository.categories,
                mediaFeedRepository.hasMoreCategories,
                categoryPreferenceRepository.getCategoryPreference(),
                categoryLabels,
            ) { categories, hasMore, preference, labels ->
                CategoryListing(categories, hasMore, preference, labels)
            }

            // flowext's combine rather than the one in kotlinx: the typed overloads there stop at
            // five flows, and this state has more parts than that
            combine(
                mediaFeedRepository.media,
                mediaFeedRepository.hasMore,
                mediaFeedRepository.filter,
                mediaFeedRepository.showCategories,
                categoryListing,
                subjectHeading,
                thumbnailSizeFlow,
                mediaPreferenceRepository.getMediaPreference(),
                _isLoading,
            ) { media, hasMore, filter, showCategories, categoryListing, heading, thumbnailSize, mediaPref, isLoading ->
                // whichever listing is on screen is the one the loading, empty and paging flags
                // are about
                val isListingEmpty = when {
                    showCategories -> categoryListing.categories.isEmpty()
                    else -> media.isEmpty()
                }

                MediaFeedUiState(
                    title = heading.title,
                    placeChain = heading.placeChain,
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
                    showCategories = showCategories,
                    categories = categoryListing.categories,
                    categoryPreference = categoryListing.preference,
                    categoryLabels = categoryListing.labels,
                    hasMore = if (showCategories) categoryListing.hasMore else hasMore,
                    isLoading = isLoading && isListingEmpty,
                    isEmpty = !isLoading && isListingEmpty,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

    fun initState(subject: MediaFeedSubject) {
            if (_subject.value == subject) {
                return
            }

            _subject.update { subject }
        mediaFeedRepository.initialize(subject)

            ensureSubjectIsNamed(subject)
            loadNextPage()
        }

        fun loadNextPage() {
            viewModelScope.launch {
                val listing = when {
                    mediaFeedRepository.showCategories.value -> mediaFeedRepository.loadNextPageOfCategories()
                    else -> mediaFeedRepository.loadNextPage()
                }

                listing.collect { status ->
                    if (status !is ExternalCallStatus.Loading) {
                        _isLoading.update { false }
                    }
                }

                // a call the repository declined - already loading, or nothing more to fetch -
                // emits nothing at all, which must still clear the initial loading state
                _isLoading.update { false }
            }
        }

        /**
         * Switches between the media the subject appears in and the categories it sits in.
         *
         * Each listing keeps what it has already accumulated, so switching back and forth costs a
         * request only the first time each is asked for.
         */
        fun setShowCategories(showCategories: Boolean) {
            if (mediaFeedRepository.showCategories.value == showCategories) {
                return
            }

            _isLoading.update { true }
            mediaFeedRepository.setShowCategories(showCategories)

            loadNextPage()
        }

    /**
     * Turns the year or the title on the listed categories on and off, for the area this feed is
     * being browsed from.
     *
     * Saved rather than held, so this and the switch on the settings screen are the one setting.
     * Nothing is refetched: it changes what a category says about itself, not which ones came back,
     * so the accumulated pages stand.
     */
    fun setShowCategoryYear(showYear: Boolean) {
        viewModelScope.launch {
            when (_subject.value) {
                is MediaFeedSubject.Place -> placePreferenceRepository.setShowCategoryYear(showYear)
                else -> peoplePreferenceRepository.setShowCategoryYear(showYear)
            }
        }
    }

    fun setShowCategoryTitle(showTitle: Boolean) {
        viewModelScope.launch {
            when (_subject.value) {
                is MediaFeedSubject.Place -> placePreferenceRepository.setShowCategoryTitle(showTitle)
                else -> peoplePreferenceRepository.setShowCategoryTitle(showTitle)
            }
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

                mediaFeedRepository.updateMedia(media.copy(isFavorite = isFavorite))
            }
        }

    fun toggleCategoryFavorite(category: Category) {
        viewModelScope.launch {
            categoryRepository
                .setFavorite(category.id, !category.isFavorite)
                .filterIsInstance<ExternalCallStatus.Success<Category>>()
                .catch { e -> Timber.e(e) }
                // the answer describes the category on its own terms and knows nothing of the
                // person it was listed for, so only the flag that was asked to change is taken
                // from it - the count of their media in it is left as it was
                .collect { status ->
                    mediaFeedRepository.updateCategory(
                        category.copy(isFavorite = status.result.isFavorite),
                    )
                }
        }
    }

    private fun applyFilter(update: (MediaFeedFilter) -> MediaFeedFilter) {
        val next = update(mediaFeedRepository.filter.value)

        if (next == mediaFeedRepository.filter.value) {
                return
            }

            _isLoading.update { true }
        mediaFeedRepository.setFilter(next)

            loadNextPage()
        }

    // the name comes from a listing this screen may have been opened without: a cold start
    // restoring straight into a feed holds none of them yet
    private fun ensureSubjectIsNamed(subject: MediaFeedSubject) {
            viewModelScope.launch {
                when (subject) {
                    is MediaFeedSubject.Person -> peopleRepository.getPeople().collect { }

                    is MediaFeedSubject.Clan -> clanRepository.getClans().collect { }

                    // one place rather than a listing: the tree is drilled through a level at a
                    // time, so there is no one list a place is always found in
                    is MediaFeedSubject.Place -> placeRepository.getPlace(subject.placeId).collect { }
                }
            }
        }
    }
