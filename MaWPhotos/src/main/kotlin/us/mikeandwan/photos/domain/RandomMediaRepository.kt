package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media

@Singleton
class RandomMediaRepository
    @Inject
    constructor(
        private val api: MediaApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            const val ERR_MSG_FETCH = "Unable to fetch random photos at this time.  Please try again later."
            private const val PAGE_SIZE = 24
        }

        private val _media = MutableStateFlow(emptyList<Media>())
        val media = _media.asStateFlow()

        // a grid can ask for the next page more than once before the first ask lands
        private val _isLoadingNextPage = MutableStateFlow(false)
        val isLoadingNextPage = _isLoadingNextPage.asStateFlow()

        // leaves the listing alone: what the widget shows has nothing to do with the Random screen
        fun fetch(count: Int) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getRandomMedia(count)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_FETCH))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_FETCH))
                    }

                    is ApiResult.Success -> {
                        emit(ExternalCallStatus.Success(result.result.map { it.toDomainMedia() }))
                    }
                }
            }

        fun loadNextPage() =
            flow {
                if (_isLoadingNextPage.value) {
                    return@flow
                }

                _isLoadingNextPage.update { true }

                try {
                    emitAll(
                        fetch(PAGE_SIZE).onEach { status ->
                            if (status is ExternalCallStatus.Success) {
                                // every request draws from the whole library, so it can repeat
                                // something already here - and a listing keyed by id cannot hold
                                // the same id twice
                                _media.update { current -> (current + status.result).distinctBy { it.id } }
                            }
                        },
                    )
                } finally {
                    _isLoadingNextPage.update { false }
                }
            }

        fun clear() {
            _media.update { emptyList() }
        }

        fun updateMedia(updated: Media) {
            _media.update { currentList ->
                val index = currentList.indexOfFirst { it.id == updated.id }

                if (index < 0) {
                    currentList
                } else {
                    currentList.toMutableList().also { it[index] = updated }
                }
            }
        }
    }
