package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.services.MediaFavoriteService

data class RandomUiState(
    val media: List<Media> = emptyList(),
    val loadFailed: Boolean = false,
)

@HiltViewModel
class RandomViewModel
    @Inject
    constructor(
        randomMediaRepository: RandomMediaRepository,
        private val mediaFavoriteService: MediaFavoriteService,
    ) : BaseRandomViewModel(
            randomMediaRepository,
        ) {
        val uiState = combine(media, randomMediaRepository.loadFailed) { media, loadFailed ->
            RandomUiState(media = media, loadFailed = loadFailed)
        }
            // starts from what is already loaded: the screen asks for a page when it sees an empty
            // list, and coming back to media already fetched should not look like one
            .stateIn(viewModelScope, WhileSubscribed(5000), RandomUiState(media = media.value))

        fun retryLoad() {
            viewModelScope.launch {
                randomMediaRepository
                    .retryNextPage()
                    .collect { }
            }
        }

        fun toggleFavorite(media: Media) {
            viewModelScope.launch {
                val isFavorite = mediaFavoriteService.setIsFavorite(media, !media.isFavorite)

                randomMediaRepository.updateMedia(media.copy(isFavorite = isFavorite))
            }
        }
    }
