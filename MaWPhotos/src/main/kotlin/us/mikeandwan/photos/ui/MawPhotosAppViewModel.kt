package us.mikeandwan.photos.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DrawerValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.models.ErrorMessage
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.components.topbar.TopBarState
import us.mikeandwan.photos.ui.screens.categories.CategoriesNavKey
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserNavKey
import us.mikeandwan.photos.ui.screens.upload.UploadNavKey
import us.mikeandwan.photos.workers.UploadWorker

@HiltViewModel
class MawPhotosAppViewModel
    @Inject
    constructor(
        private val errorRepository: ErrorRepository,
        private val categoryRepository: CategoryRepository,
        authService: AuthService,
        private val configRepository: ConfigRepository,
        private val application: Application,
        private val fileStorageRepository: FileStorageRepository,
        private val searchRepository: SearchRepository,
        private val randomMediaRepository: RandomMediaRepository,
    ) : ViewModel() {
        val authenticationStatus = authService.authStatus
        val userStatus = configRepository.userStatus
        val years = categoryRepository.getYears()

        private val _activeYear = MutableStateFlow(-1)
        val activeYear = _activeYear.asStateFlow()

        private val _navArea = MutableStateFlow(NavigationArea.Category)
        val navArea = _navArea.asStateFlow()

        private val _topBarState = MutableStateFlow(TopBarState())
        val topBarState = _topBarState.asStateFlow()

        val enableDrawerGestures = topBarState
            .map { it.show && it.showAppIcon }
            .stateIn(viewModelScope, WhileSubscribed(5000), true)

        private val _drawerState = MutableStateFlow(DrawerValue.Closed)
        val drawerState = _drawerState.asStateFlow()

        private val _navigationEvents = Channel<NavKey>(Channel.BUFFERED)
        val navigationEvents = _navigationEvents.receiveAsFlow()

        val errorsToDisplay = errorRepository.error
            .filter { it is ErrorMessage.Display }
            .map { it as ErrorMessage.Display }

        val recentSearchTerms = searchRepository
            .getSearchHistory()
            .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

        fun setNavArea(area: NavigationArea) {
            _navArea.value = area
        }

        fun openDrawer() {
            _drawerState.value = DrawerValue.Open
        }

        fun closeDrawer() {
            _drawerState.value = DrawerValue.Closed
        }

        fun navigate(route: NavKey) {
            viewModelScope.launch {
                _navigationEvents.send(route)
            }
        }

        fun updateTopBar(
            navArea: NavigationArea,
            nextState: TopBarState,
        ) {
            // added the navarea param so callers can identify where they are coming from.  in particular, this
            // guards against cases where the nav area is changed but we get a late request to update the top nav.
            // this easily happened when on a category and then go to perform an upload.  handling the intent for the
            // send/upload results in the upload page firing first, but the category screen loads data late as it is
            // the starting route and would often result in the year at the top of the upload page...
            if (navArea == _navArea.value) {
                _topBarState.value = nextState
            }
        }

        fun setActiveYear(year: Int) {
            _activeYear.value = year
        }

        fun clearSearchHistory() {
            viewModelScope.launch {
                searchRepository.clearHistory()
            }
        }

        fun fetchRandomPhotos(count: Int) {
            viewModelScope.launch {
                randomMediaRepository
                    .fetch(count)
                    .collect { }
            }

            closeDrawer()
        }

        fun clearRandomPhotos() {
            randomMediaRepository.clear()
            closeDrawer()
        }

        fun handleIntent(intent: Intent?) {
            if (intent == null) {
                navigate(CategoriesNavKey(null))
                return
            }

            when (intent.action) {
                Intent.ACTION_SEND -> {
                    handleSendSingle(intent)
                    navigate(UploadNavKey)
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    handleSendMultiple(intent)
                    navigate(UploadNavKey)
                }

                else -> {
                    viewModelScope.launch {
                        combine(
                            userStatus,
                            authenticationStatus,
                        ) { userStatus, authStatus ->
                            if (userStatus is UserStatus.Unknown && authStatus is AuthStatus.Authorized) {
                                queryUserStatus()
                            }

                            if (userStatus is UserStatus.Inactive) {
                                Timber.w("YO INACTIVE!")
                                navigate(InactiveUserNavKey)
                            }
                        }.collect { }
                    }
                }
            }
        }

        private fun handleSendSingle(intent: Intent) {
            val mediaUri = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java,
            )

            if (mediaUri != null) {
                enqueueUpload(mediaUri)
            }
        }

        private fun handleSendMultiple(intent: Intent) {
            val mediaUris = IntentCompat.getParcelableArrayListExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java,
            )

            if (mediaUris != null) {
                enqueueUpload(*mediaUris.toTypedArray())
            }
        }

        fun queryUserStatus() {
            viewModelScope.launch {
                configRepository.getUserStatus()
            }
        }

        private fun enqueueUpload(vararg mediaUri: Uri) {
            viewModelScope.launch {
                mediaUri.forEach {
                    val file = saveUploadFile(it)

                    if (file != null) {
                        val constraints = Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .build()

                        val data = workDataOf(
                            UploadWorker.KEY_FILENAME to file.path,
                        )

                        val work = OneTimeWorkRequestBuilder<UploadWorker>()
                            .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                1,
                                TimeUnit.MINUTES,
                            ).setConstraints(constraints)
                            .setInputData(data)
                            .build()

                        val workManager = WorkManager.getInstance(application)

                        workManager.enqueueUniqueWork(
                            "upload ${file.path}",
                            ExistingWorkPolicy.REPLACE,
                            work,
                        )
                    }
                }
            }
        }

        private suspend fun clearFileCache() {
            fileStorageRepository.clearLegacyDatabase()
            fileStorageRepository.clearShareCache()
            fileStorageRepository.clearLegacyFiles()
        }

        private suspend fun saveUploadFile(mediaUri: Uri): File? = fileStorageRepository.saveFileToUpload(mediaUri)

        private fun bootstrapAppData() {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val loaded = withTimeoutOrNull(10_000) {
                        configRepository.getScales().first { it.isNotEmpty() }
                    }

                    if (loaded == null) {
                        errorRepository.logError("MawPhotosAppViewModel: Scales did not load within timeout")
                    }

                    val years = categoryRepository.getYears().first()

                    if (years.isEmpty()) {
                        categoryRepository.loadYears(null).collect { }
                    }

                    val finalYears = categoryRepository.getYears().first()
                    val targetYear = finalYears.maxOrNull()

                    if (targetYear != null) {
                        val cats = categoryRepository.getCategories(targetYear).first()
                        if (cats.isEmpty()) {
                            categoryRepository.loadCategories(targetYear).collect { /* no-op */ }
                        }
                    }
                } catch (e: Exception) {
                    errorRepository.logError(
                        "MawPhotosAppViewModel: Error loading scales/years/categories after auth",
                        e,
                    )
                }
            }
        }

        init {
            viewModelScope.launch {
                fileStorageRepository.refreshPendingUploads()
                clearFileCache()
            }

            viewModelScope.launch {
                authenticationStatus.collect { status ->
                    if (status is AuthStatus.Authorized) {
                        bootstrapAppData()
                    }
                }
            }
        }
    }
