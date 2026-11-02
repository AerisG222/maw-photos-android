package us.mikeandwan.photos.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.api.SearchResults
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFeedFilter
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.toApiFilter
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Media as ApiMedia

/**
 * What one person appears in, what anyone in a clan appears in, or what was taken at one place -
 * accumulated a page at a time.
 *
 * Two listings over the one subject: the media itself, and the categories that media sits in. Both
 * are held so that switching between them keeps what has already been scrolled through.
 *
 * Handed out by [MediaFeedRepository], which is where the grid and the pager find the same one.
 */
class MediaFeed internal constructor(
    val subject: MediaFeedSubject,
    private val faceApi: FaceApiClient,
    private val placeApi: PlaceApiClient,
    apiErrorHandler: ApiErrorHandler,
    filter: MediaFeedFilter,
) {
    private companion object {
        const val ERR_MSG_LOAD_MEDIA = "Unable to load media at this time.  Please try again later."
        const val ERR_MSG_LOAD_CATEGORIES = "Unable to load categories at this time.  Please try again later."
    }

    private val mediaPager = ListingPager<ApiMedia, Media>(
        apiErrorHandler = apiErrorHandler,
        errorMessage = ERR_MSG_LOAD_MEDIA,
        idOf = { it.id },
        toDomain = { it.toDomainMedia() },
    )

    private val categoryPager = ListingPager<ApiCategory, Category>(
        apiErrorHandler = apiErrorHandler,
        errorMessage = ERR_MSG_LOAD_CATEGORIES,
        idOf = { it.id },
        toDomain = { it.toDomainCategory() },
    )

    val media = mediaPager.items
    val hasMore = mediaPager.hasMore
    val isLoadingMedia = mediaPager.isLoading
    val mediaLoadFailed = mediaPager.loadFailed

    val categories = categoryPager.items
    val hasMoreCategories = categoryPager.hasMore
    val categoriesLoadFailed = categoryPager.loadFailed

    private val _filter = MutableStateFlow(filter)
    val filter = _filter.asStateFlow()

    fun hasStartedListing(showCategories: Boolean) =
        if (showCategories) categoryPager.hasStarted else mediaPager.hasStarted

    // narrowing to favorites or reshuffling changes which rows come back and in what order, so
    // the accumulated list cannot be kept
    fun setFilter(filter: MediaFeedFilter) {
        val current = _filter.value

        if (current == filter) {
            return
        }

        _filter.update { filter }

        mediaPager.reset()

        // the seed only orders media - there is no shuffling a list of categories, and the API
        // takes no seed for one - so the categories are only started over when the narrowing
        // itself changed
        if (current.mediaFilters != filter.mediaFilters) {
            categoryPager.reset()
        }
    }

    fun loadNextPage() = mediaPager.loadNextPage { offset -> fetchMedia(offset) }

    fun retryLoadNextPage() = mediaPager.retry { offset -> fetchMedia(offset) }

    fun loadNextPageOfCategories() = categoryPager.loadNextPage { offset -> fetchCategories(offset) }

    fun retryLoadNextPageOfCategories() = categoryPager.retry { offset -> fetchCategories(offset) }

    fun updateMedia(updated: Media) {
        mediaPager.update(updated)
    }

    fun updateCategory(updated: Category) {
        categoryPager.update(updated)
    }

    private suspend fun fetchMedia(offset: Int): ApiResult<SearchResults<ApiMedia>> {
        val filter = _filter.value
        val mediaType = filter.mediaType?.toApiFilter()

        return when (subject) {
            is MediaFeedSubject.Person -> {
                faceApi.getPersonMedia(subject.personId, offset, filter.favoritesOnly, filter.seed, mediaType)
            }

            is MediaFeedSubject.Clan -> {
                faceApi.getClanMedia(subject.clanId, offset, filter.favoritesOnly, filter.seed, mediaType)
            }

            is MediaFeedSubject.Place -> {
                placeApi.getPlaceMedia(subject.placeId, offset, filter.favoritesOnly, filter.seed, mediaType)
            }
        }
    }

    private suspend fun fetchCategories(offset: Int): ApiResult<SearchResults<ApiCategory>> {
        val favoritesOnly = _filter.value.favoritesOnly
        val mediaType = _filter.value.mediaType?.toApiFilter()

        return when (subject) {
            is MediaFeedSubject.Person -> {
                faceApi.getPersonCategories(subject.personId, offset, favoritesOnly, mediaType)
            }

            is MediaFeedSubject.Clan -> {
                faceApi.getClanCategories(subject.clanId, offset, favoritesOnly, mediaType)
            }

            is MediaFeedSubject.Place -> {
                placeApi.getPlaceCategories(subject.placeId, offset, favoritesOnly, mediaType)
            }
        }
    }
}
