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
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryPreference
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
    // whether the categories the subject turns up in are being listed rather than the media itself
    val showCategories: Boolean = false,
    val categories: List<Category> = emptyList(),
    // the categories are drawn the way the rest of the app draws categories, so whoever prefers a
    // list of them to a wall of teasers gets one here too
    val categoryPreference: CategoryPreference = CategoryPreference(),
    // both of these describe whichever listing is on screen, so the screen does not have to ask
    // which one it is showing before it can page or say there is nothing here
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    // told apart from loading so the screen can say there is nothing here rather than spin forever
    val isEmpty: Boolean = false,
)

// the parts of the categories listing, folded together so the state above can be built in one
// combine rather than two
private data class CategoryListing(
    val categories: List<Category> = emptyList(),
    val hasMore: Boolean = false,
    val preference: CategoryPreference = CategoryPreference(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FaceFeedViewModel
    @Inject
    constructor(
        private val faceFeedRepository: FaceFeedRepository,
        private val peopleRepository: PeopleRepository,
        private val clanRepository: ClanRepository,
        private val categoryRepository: CategoryRepository,
        categoryPreferenceRepository: CategoryPreferenceRepository,
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

            // kotlinx's combine here - flowext's overloads start at six flows
            val categoryListing = combine(
                faceFeedRepository.categories,
                faceFeedRepository.hasMoreCategories,
                categoryPreferenceRepository.getCategoryPreference(),
            ) { categories, hasMore, preference ->
                CategoryListing(categories, hasMore, preference)
            }

            // flowext's combine rather than the one in kotlinx: the typed overloads there stop at
            // five flows, and this state has more parts than that
            combine(
                faceFeedRepository.media,
                faceFeedRepository.hasMore,
                faceFeedRepository.filter,
                faceFeedRepository.showCategories,
                categoryListing,
                title,
                thumbnailSizeFlow,
                mediaPreferenceRepository.getMediaPreference(),
                _isLoading,
            ) { media, hasMore, filter, showCategories, categoryListing, title, thumbnailSize, mediaPref, isLoading ->
                // whichever listing is on screen is the one the loading, empty and paging flags
                // are about
                val isListingEmpty = when {
                    showCategories -> categoryListing.categories.isEmpty()
                    else -> media.isEmpty()
                }

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
                    showCategories = showCategories,
                    categories = categoryListing.categories,
                    categoryPreference = categoryListing.preference,
                    hasMore = if (showCategories) categoryListing.hasMore else hasMore,
                    isLoading = isLoading && isListingEmpty,
                    isEmpty = !isLoading && isListingEmpty,
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
                val listing = when {
                    faceFeedRepository.showCategories.value -> faceFeedRepository.loadNextPageOfCategories()
                    else -> faceFeedRepository.loadNextPage()
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
            if (faceFeedRepository.showCategories.value == showCategories) {
                return
            }

            _isLoading.update { true }
            faceFeedRepository.setShowCategories(showCategories)

        loadNextPage()
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
                    faceFeedRepository.updateCategory(
                        category.copy(isFavorite = status.result.isFavorite),
                    )
                }
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
