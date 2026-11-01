package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.services.MediaFavoriteService

data class RandomUiState(
    val media: List<Media> = emptyList(),
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
        val uiState = media
            .map { RandomUiState(media = it) }
            // starts from what is already loaded: the screen asks for a page when it sees an empty
            // list, and coming back to media already fetched should not look like one
            .stateIn(viewModelScope, WhileSubscribed(5000), RandomUiState(media = media.value))

        fun toggleFavorite(media: Media) {
            viewModelScope.launch {
                val isFavorite = mediaFavoriteService.setIsFavorite(media, !media.isFavorite)

                randomMediaRepository.updateMedia(media.copy(isFavorite = isFavorite))
            }
        }
    }
