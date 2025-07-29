package us.mikeandwan.photos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (
    private val authService: AuthService,
    private val categoryPreferenceRepository: CategoryPreferenceRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val mediaPreferenceRepository: MediaPreferenceRepository,
    private val randomPreferenceRepository: RandomPreferenceRepository,
    private val searchPreferenceRepository: SearchPreferenceRepository,
    private val errorRepository: ErrorRepository
): ViewModel() {
    val notificationDoNotify = notificationPreferenceRepository
        .getDoNotify()
        .stateIn(viewModelScope, WhileSubscribed(5000), false)

    val notificationDoVibrate = notificationPreferenceRepository
        .getDoVibrate()
        .stateIn(viewModelScope, WhileSubscribed(5000), true)

    val categoryDisplayType = categoryPreferenceRepository
        .getCategoryDisplayType()
        .stateIn(viewModelScope, WhileSubscribed(5000), CategoryDisplayType.Grid)

    val categoryThumbnailSize = categoryPreferenceRepository
        .getCategoryGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    val photoSlideshowInterval = mediaPreferenceRepository
        .getSlideshowIntervalSeconds()
        .stateIn(viewModelScope, WhileSubscribed(5000), 3)

    val photoThumbnailSize = mediaPreferenceRepository
        .getPhotoGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    val randomSlideshowInterval = randomPreferenceRepository
        .getSlideshowIntervalSeconds()
        .stateIn(viewModelScope, WhileSubscribed(5000), 3)

    val randomThumbnailSize = randomPreferenceRepository
        .getPhotoGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    val searchQueryCount = searchPreferenceRepository
        .getSearchesToSaveCount()
        .stateIn(viewModelScope, WhileSubscribed(5000), 20)

    val searchDisplayType = searchPreferenceRepository
        .getSearchDisplayType()
        .stateIn(viewModelScope, WhileSubscribed(5000), CategoryDisplayType.Grid)

    val searchThumbnailSize = searchPreferenceRepository
        .getSearchGridItemSize()
        .stateIn(viewModelScope, WhileSubscribed(5000), GridThumbnailSize.Medium)

    fun setNotificationDoNotify(doNotify: Boolean) {
        viewModelScope.launch {
            notificationPreferenceRepository.setDoNotify(doNotify)
        }
    }

    fun setNotificationDoVibrate(doVibrate: Boolean) {
        viewModelScope.launch {
            notificationPreferenceRepository.setDoVibrate(doVibrate)
        }
    }

    fun setCategoryDisplayType(categoryDisplayType: CategoryDisplayType) {
        viewModelScope.launch {
            categoryPreferenceRepository.setCategoryDisplayType(categoryDisplayType)
        }
    }

    fun setCategoryThumbnailSize(categoryThumbnailSize: GridThumbnailSize) {
        viewModelScope.launch {
            categoryPreferenceRepository.setCategoryGridItemSize(categoryThumbnailSize)
        }
    }

    fun setPhotoSlideshowInterval(slideshowInterval: Int) {
        viewModelScope.launch {
            mediaPreferenceRepository.setSlideshowIntervalSeconds(slideshowInterval)
        }
    }

    fun setPhotoThumbnailSize(photoThumbnailSize: GridThumbnailSize) {
        viewModelScope.launch {
            mediaPreferenceRepository.setPhotoGridItemSize(photoThumbnailSize)
        }
    }

    fun setRandomSlideshowInterval(slideshowInterval: Int) {
        viewModelScope.launch {
            randomPreferenceRepository.setSlideshowIntervalSeconds(slideshowInterval)
        }
    }

    fun setRandomThumbnailSize(randomThumbnailSize: GridThumbnailSize) {
        viewModelScope.launch {
            randomPreferenceRepository.setPhotoGridItemSize(randomThumbnailSize)
        }
    }

    fun setSearchQueryCount(searchQueryCount: Int) {
        viewModelScope.launch {
            searchPreferenceRepository.setSearchesToSaveCount(searchQueryCount)
        }
    }

    fun setSearchDisplayType(searchDisplayType: CategoryDisplayType) {
        viewModelScope.launch {
            searchPreferenceRepository.setSearchDisplayType(searchDisplayType)
        }
    }

    fun setSearchThumbnailSize(searchThumbnailSize: GridThumbnailSize) {
        viewModelScope.launch {
            searchPreferenceRepository.setSearchGridItemSize(searchThumbnailSize)
        }
    }

    fun showError(message: String) {
        errorRepository.showError(message)
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            authService.logout(context)
        }
    }
}
