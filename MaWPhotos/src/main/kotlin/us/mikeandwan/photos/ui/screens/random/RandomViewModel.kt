package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import javax.inject.Inject

@HiltViewModel
class RandomViewModel @Inject constructor(
    authGuard: AuthGuard,
    randomMediaRepository: RandomMediaRepository,
    randomPreferenceRepository: RandomPreferenceRepository,
) : BaseRandomViewModel(
    randomMediaRepository
) {
    val gridItemThumbnailSize = randomPreferenceRepository
        .getPhotoGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    val isAuthorized = authGuard.status
        .map {
            when(it) {
                is GuardStatus.Failed -> false
                else -> true
            }
        }.stateIn(viewModelScope, WhileSubscribed(5000), true)

    fun initialFetch(count: Int) {
        // prevent fetching a new full amount after navigating between item and list views
        if(media.value.size < count) {
            fetch(count)
        }
    }
}
