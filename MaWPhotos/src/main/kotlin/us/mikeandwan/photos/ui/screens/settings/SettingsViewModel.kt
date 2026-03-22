package us.mikeandwan.photos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.database.DeveloperLog
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize

data class SettingsUiState(
    val notificationDoNotify: Boolean = false,
    val notificationDoVibrate: Boolean = true,
    val categoryDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val categoryThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val categoryShowMediaTypeIndicator: Boolean = true,
    val photoSlideshowInterval: Int = 3,
    val photoThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val photoShowMediaTypeIndicator: Boolean = true,
    val randomSlideshowInterval: Int = 3,
    val randomThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val randomShowMediaTypeIndicator: Boolean = true,
    val searchQueryCount: Int = 20,
    val searchDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val searchThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val searchShowMediaTypeIndicator: Boolean = true,
    val isDeveloperMode: Boolean = false,
    val developerLogs: List<DeveloperLog> = emptyList(),
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authService: AuthService,
        private val categoryPreferenceRepository: CategoryPreferenceRepository,
        private val notificationPreferenceRepository: NotificationPreferenceRepository,
        private val mediaPreferenceRepository: MediaPreferenceRepository,
        private val randomPreferenceRepository: RandomPreferenceRepository,
        private val searchPreferenceRepository: SearchPreferenceRepository,
        private val errorRepository: ErrorRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState = _uiState.asStateFlow()

        init {
            combine(
                notificationPreferenceRepository.getDoNotify(),
                notificationPreferenceRepository.getDoVibrate(),
                categoryPreferenceRepository.getCategoryDisplayType(),
                categoryPreferenceRepository.getCategoryGridItemSize(),
                categoryPreferenceRepository.getCategoryPreference(),
                mediaPreferenceRepository.getSlideshowIntervalSeconds(),
                mediaPreferenceRepository.getPhotoGridItemSize(),
                mediaPreferenceRepository.getMediaPreference(),
                randomPreferenceRepository.getSlideshowIntervalSeconds(),
                randomPreferenceRepository.getPhotoGridItemSize(),
                randomPreferenceRepository.getRandomPreferences(),
                searchPreferenceRepository.getSearchesToSaveCount(),
                searchPreferenceRepository.getSearchDisplayType(),
                searchPreferenceRepository.getSearchGridItemSize(),
                searchPreferenceRepository.getSearchPreference(),
                errorRepository.isDeveloperMode,
                errorRepository.developerLogs,
            ) { args: Array<Any?> ->
                SettingsUiState(
                    notificationDoNotify = args[0] as Boolean,
                    notificationDoVibrate = args[1] as Boolean,
                    categoryDisplayType = args[2] as CategoryDisplayType,
                    categoryThumbnailSize = args[3] as GridThumbnailSize,
                    categoryShowMediaTypeIndicator = (args[4] as us.mikeandwan.photos.domain.models.CategoryPreference)
                        .showMediaTypeIndicator,
                    photoSlideshowInterval = args[5] as Int,
                    photoThumbnailSize = args[6] as GridThumbnailSize,
                    photoShowMediaTypeIndicator = (args[7] as us.mikeandwan.photos.domain.models.MediaPreference)
                        .showMediaTypeIndicator,
                    randomSlideshowInterval = args[8] as Int,
                    randomThumbnailSize = args[9] as GridThumbnailSize,
                    randomShowMediaTypeIndicator = (args[10] as us.mikeandwan.photos.domain.models.RandomPreference)
                        .showMediaTypeIndicator,
                    searchQueryCount = args[11] as Int,
                    searchDisplayType = args[12] as CategoryDisplayType,
                    searchThumbnailSize = args[13] as GridThumbnailSize,
                    searchShowMediaTypeIndicator = (args[14] as us.mikeandwan.photos.domain.models.SearchPreference)
                        .showMediaTypeIndicator,
                    isDeveloperMode = args[15] as Boolean,
                    developerLogs = args[16] as List<DeveloperLog>,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)
        }

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

        fun setCategoryShowMediaTypeIndicator(show: Boolean) {
            viewModelScope.launch {
                categoryPreferenceRepository.setShowMediaTypeIndicator(show)
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

        fun setPhotoShowMediaTypeIndicator(show: Boolean) {
            viewModelScope.launch {
                mediaPreferenceRepository.setShowMediaTypeIndicator(show)
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

        fun setRandomShowMediaTypeIndicator(show: Boolean) {
            viewModelScope.launch {
                randomPreferenceRepository.setShowMediaTypeIndicator(show)
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

        fun setSearchShowMediaTypeIndicator(show: Boolean) {
            viewModelScope.launch {
                searchPreferenceRepository.setShowMediaTypeIndicator(show)
            }
        }

        fun toggleDeveloperMode(code: String) {
            if (errorRepository.toggleDeveloperMode(code)) {
                val msg = if (errorRepository.isDeveloperMode.value) {
                    "Developer mode enabled"
                } else {
                    "Developer mode disabled"
                }

                errorRepository.showError(msg)
            } else {
                errorRepository.showError("Invalid developer code")
            }
        }

        fun clearLogs() {
            viewModelScope.launch {
                errorRepository.clearLogs()
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
