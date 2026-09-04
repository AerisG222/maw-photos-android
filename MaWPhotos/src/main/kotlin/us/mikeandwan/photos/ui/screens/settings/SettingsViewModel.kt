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
import us.mikeandwan.photos.authorization.ScopeAccess
import us.mikeandwan.photos.database.DeveloperLog
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PlacePreferenceRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PlacePreference

data class SettingsUiState(
    val notificationDoNotify: Boolean = false,
    val notificationDoVibrate: Boolean = true,
    val categoryDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val categoryThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val categoryShowMediaTypeIndicator: Boolean = true,
    val categoryShowFavoriteIndicator: Boolean = true,
    val photoSlideshowInterval: Int = 3,
    val photoThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val photoShowMediaTypeIndicator: Boolean = true,
    val photoShowFavoriteIndicator: Boolean = true,
    val randomSlideshowInterval: Int = 3,
    val randomThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val randomShowMediaTypeIndicator: Boolean = true,
    val randomShowFavoriteIndicator: Boolean = true,
    val randomShowWidgetInfo: Boolean = true,
    val searchQueryCount: Int = 20,
    val searchDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val searchThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val searchShowMediaTypeIndicator: Boolean = true,
    val searchShowFavoriteIndicator: Boolean = true,
    val peopleThumbnailSize: GridThumbnailSize = GridThumbnailSize.Medium,
    val peopleShowNames: Boolean = true,
    val peopleShowMediaCounts: Boolean = true,
    val peopleShowClans: Boolean = true,
    // what a category says about itself when a person's, a clan's or a place's categories are being
    // listed.  kept per area, because the two are read differently - see CategoryLabels
    val peopleShowCategoryYear: Boolean = true,
    val peopleShowCategoryTitle: Boolean = true,
    val placeShowCategoryYear: Boolean = true,
    val placeShowCategoryTitle: Boolean = true,
    // stored with the media preferences rather than the people ones - it applies wherever media is
    // shown - but offered beside the rest of the face settings, since it is face data it draws
    val mediaShowFaceHighlights: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val developerLogs: List<DeveloperLog> = emptyList(),
    val faceRecognitionAccess: ScopeAccess = ScopeAccess.Unknown,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authService: AuthService,
        private val categoryPreferenceRepository: CategoryPreferenceRepository,
        private val notificationPreferenceRepository: NotificationPreferenceRepository,
        private val mediaPreferenceRepository: MediaPreferenceRepository,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
        private val placePreferenceRepository: PlacePreferenceRepository,
        private val randomPreferenceRepository: RandomPreferenceRepository,
        private val searchPreferenceRepository: SearchPreferenceRepository,
        private val widgetRandomPhotoService: us.mikeandwan.photos.domain.services.WidgetRandomPhotoService,
        private val fileStorageRepository: FileStorageRepository,
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
                authService.faceRecognitionAccess,
                peoplePreferenceRepository.getPeoplePreference(),
                placePreferenceRepository.getPlacePreference(),
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val developerLogs = args[16] as List<DeveloperLog>
                val peoplePreference = args[18] as PeoplePreference
                val placePreference = args[19] as PlacePreference

                SettingsUiState(
                    notificationDoNotify = args[0] as Boolean,
                    notificationDoVibrate = args[1] as Boolean,
                    categoryDisplayType = args[2] as CategoryDisplayType,
                    categoryThumbnailSize = args[3] as GridThumbnailSize,
                    categoryShowMediaTypeIndicator = (args[4] as us.mikeandwan.photos.domain.models.CategoryPreference)
                        .showMediaTypeIndicator,
                    categoryShowFavoriteIndicator = (args[4] as us.mikeandwan.photos.domain.models.CategoryPreference)
                        .showFavoriteIndicator,
                    photoSlideshowInterval = args[5] as Int,
                    photoThumbnailSize = args[6] as GridThumbnailSize,
                    photoShowMediaTypeIndicator = (args[7] as us.mikeandwan.photos.domain.models.MediaPreference)
                        .showMediaTypeIndicator,
                    photoShowFavoriteIndicator = (args[7] as us.mikeandwan.photos.domain.models.MediaPreference)
                        .showFavoriteIndicator,
                    randomSlideshowInterval = args[8] as Int,
                    randomThumbnailSize = args[9] as GridThumbnailSize,
                    randomShowMediaTypeIndicator = (args[10] as us.mikeandwan.photos.domain.models.RandomPreference)
                        .showMediaTypeIndicator,
                    randomShowFavoriteIndicator = (args[10] as us.mikeandwan.photos.domain.models.RandomPreference)
                        .showFavoriteIndicator,
                    randomShowWidgetInfo = (args[10] as us.mikeandwan.photos.domain.models.RandomPreference)
                        .showWidgetInfo,
                    searchQueryCount = args[11] as Int,
                    searchDisplayType = args[12] as CategoryDisplayType,
                    searchThumbnailSize = args[13] as GridThumbnailSize,
                    searchShowMediaTypeIndicator = (args[14] as us.mikeandwan.photos.domain.models.SearchPreference)
                        .showMediaTypeIndicator,
                    searchShowFavoriteIndicator = (args[14] as us.mikeandwan.photos.domain.models.SearchPreference)
                        .showFavoriteIndicator,
                    peopleThumbnailSize = peoplePreference.gridThumbnailSize,
                    peopleShowNames = peoplePreference.showNames,
                    peopleShowMediaCounts = peoplePreference.showMediaCounts,
                    peopleShowClans = peoplePreference.showClans,
                    peopleShowCategoryYear = peoplePreference.showCategoryYear,
                    peopleShowCategoryTitle = peoplePreference.showCategoryTitle,
                    placeShowCategoryYear = placePreference.showCategoryYear,
                    placeShowCategoryTitle = placePreference.showCategoryTitle,
                    mediaShowFaceHighlights = (args[7] as us.mikeandwan.photos.domain.models.MediaPreference)
                        .showFaceHighlights,
                    isDeveloperMode = args[15] as Boolean,
                    developerLogs = developerLogs,
                    faceRecognitionAccess = args[17] as ScopeAccess,
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

        fun setCategoryShowFavoriteIndicator(show: Boolean) {
            viewModelScope.launch {
                categoryPreferenceRepository.setShowFavoriteIndicator(show)
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

        fun setPhotoShowFavoriteIndicator(show: Boolean) {
            viewModelScope.launch {
                mediaPreferenceRepository.setShowFavoriteIndicator(show)
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

        fun setRandomShowFavoriteIndicator(show: Boolean) {
            viewModelScope.launch {
                randomPreferenceRepository.setShowFavoriteIndicator(show)
            }
        }

        fun setRandomShowWidgetInfo(
            show: Boolean,
            context: Context,
        ) {
            viewModelScope.launch {
                randomPreferenceRepository.setShowWidgetInfo(show)
                widgetRandomPhotoService.updateShowInfo(context, show)
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

        fun setSearchShowFavoriteIndicator(show: Boolean) {
            viewModelScope.launch {
                searchPreferenceRepository.setShowFavoriteIndicator(show)
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

        fun clearCache() {
            viewModelScope.launch {
                fileStorageRepository.clearImageCache()
                errorRepository.showError("Cache cleared")
            }
        }

        fun showError(message: String) {
            errorRepository.showError(message)
        }

        fun setPeopleThumbnailSize(size: GridThumbnailSize) {
            viewModelScope.launch {
                peoplePreferenceRepository.setPeopleGridItemSize(size)
            }
        }

        fun setPeopleShowNames(show: Boolean) {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowNames(show)
            }
        }

        fun setPeopleShowMediaCounts(show: Boolean) {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowMediaCounts(show)
            }
        }

        fun setPeopleShowCategoryYear(show: Boolean) {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowCategoryYear(show)
            }
        }

        fun setPeopleShowCategoryTitle(show: Boolean) {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowCategoryTitle(show)
            }
        }

        fun setPlaceShowCategoryYear(show: Boolean) {
            viewModelScope.launch {
                placePreferenceRepository.setShowCategoryYear(show)
            }
        }

        fun setPlaceShowCategoryTitle(show: Boolean) {
            viewModelScope.launch {
                placePreferenceRepository.setShowCategoryTitle(show)
        }
    }

        fun setPeopleShowClans(show: Boolean) {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowClans(show)
            }
        }

        fun setMediaShowFaceHighlights(show: Boolean) {
            viewModelScope.launch {
                mediaPreferenceRepository.setShowFaceHighlights(show)
            }
        }

        fun logout(context: Context) {
            viewModelScope.launch {
                authService.logout(context)
            }
        }

        // a fresh login is the only thing that can widen a grant, so an authorization the current
        // credentials do not carry is offered as signing in again rather than as a retry
        fun reauthorize(context: Context) {
            viewModelScope.launch {
                authService.login(context)
            }
        }
    }
