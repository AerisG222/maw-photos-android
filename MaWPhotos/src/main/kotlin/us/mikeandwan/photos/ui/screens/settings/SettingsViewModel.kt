package us.mikeandwan.photos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PlacePreference

data class SettingsUiState(
    val notificationDoNotify: Boolean = false,
    val notificationDoVibrate: Boolean = true,
    val categoryDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
    val photoSlideshowInterval: Int = 3,
    val randomSlideshowInterval: Int = 3,
    val randomShowWidgetInfo: Boolean = true,
    val searchQueryCount: Int = 20,
    val searchDisplayType: CategoryDisplayType = CategoryDisplayType.Grid,
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
        val uiState =
            combine(
                notificationPreferenceRepository.getDoNotify(),
                notificationPreferenceRepository.getDoVibrate(),
                categoryPreferenceRepository.getCategoryDisplayType(),
                mediaPreferenceRepository.getSlideshowIntervalSeconds(),
                mediaPreferenceRepository.getMediaPreference(),
                randomPreferenceRepository.getSlideshowIntervalSeconds(),
                randomPreferenceRepository.getRandomPreferences(),
                searchPreferenceRepository.getSearchesToSaveCount(),
                searchPreferenceRepository.getSearchDisplayType(),
                errorRepository.isDeveloperMode,
                errorRepository.developerLogs,
                authService.faceRecognitionAccess,
                peoplePreferenceRepository.getPeoplePreference(),
                placePreferenceRepository.getPlacePreference(),
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val developerLogs = args[10] as List<DeveloperLog>
                val mediaPreference = args[4] as us.mikeandwan.photos.domain.models.MediaPreference
                val randomPreference = args[6] as us.mikeandwan.photos.domain.models.RandomPreference
                val peoplePreference = args[12] as PeoplePreference
                val placePreference = args[13] as PlacePreference

                SettingsUiState(
                    notificationDoNotify = args[0] as Boolean,
                    notificationDoVibrate = args[1] as Boolean,
                    categoryDisplayType = args[2] as CategoryDisplayType,
                    photoSlideshowInterval = args[3] as Int,
                    randomSlideshowInterval = args[5] as Int,
                    randomShowWidgetInfo = randomPreference.showWidgetInfo,
                    searchQueryCount = args[7] as Int,
                    searchDisplayType = args[8] as CategoryDisplayType,
                    peopleShowNames = peoplePreference.showNames,
                    peopleShowMediaCounts = peoplePreference.showMediaCounts,
                    peopleShowClans = peoplePreference.showClans,
                    peopleShowCategoryYear = peoplePreference.showCategoryYear,
                    peopleShowCategoryTitle = peoplePreference.showCategoryTitle,
                    placeShowCategoryYear = placePreference.showCategoryYear,
                    placeShowCategoryTitle = placePreference.showCategoryTitle,
                    mediaShowFaceHighlights = mediaPreference.showFaceHighlights,
                    isDeveloperMode = args[9] as Boolean,
                    developerLogs = developerLogs,
                    faceRecognitionAccess = args[11] as ScopeAccess,
                )
            }.stateIn(viewModelScope, WhileSubscribed(5000), SettingsUiState())

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

        fun setPhotoSlideshowInterval(slideshowInterval: Int) {
            viewModelScope.launch {
                mediaPreferenceRepository.setSlideshowIntervalSeconds(slideshowInterval)
            }
        }

        fun setRandomSlideshowInterval(slideshowInterval: Int) {
            viewModelScope.launch {
                randomPreferenceRepository.setSlideshowIntervalSeconds(slideshowInterval)
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
