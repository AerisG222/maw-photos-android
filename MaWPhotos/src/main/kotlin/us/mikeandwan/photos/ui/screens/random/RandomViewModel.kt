package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.Media

data class RandomUiState(
    val media: List<Media> = emptyList(),
    val thumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val showMediaTypeIndicator: Boolean = true,
)

@HiltViewModel
class RandomViewModel
    @Inject
    constructor(
        randomMediaRepository: RandomMediaRepository,
        randomPreferenceRepository: RandomPreferenceRepository,
    ) : BaseRandomViewModel(
            randomMediaRepository,
        ) {
        val uiState = combine(
            media,
            randomPreferenceRepository.getPhotoGridItemSize(),
            randomPreferenceRepository.getRandomPreferences(),
        ) { media, thumbSize, randomPref ->
            RandomUiState(
                media = media,
                thumbnailSize = thumbSize,
                showMediaTypeIndicator = randomPref.showMediaTypeIndicator,
            )
        }.stateIn(viewModelScope, WhileSubscribed(5000), RandomUiState())

        fun initialFetch(count: Int) {
            // prevent fetching a new full amount after navigating between item and list views
            if (media.value.size < count) {
                fetch(count)
            }
        }
    }
