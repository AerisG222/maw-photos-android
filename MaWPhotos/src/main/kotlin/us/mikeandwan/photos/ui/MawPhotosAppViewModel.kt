package us.mikeandwan.photos.ui

import android.app.Application
import android.content.Context
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.authorization.ScopeAccess
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.models.ErrorMessage
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.components.topbar.TopBarState
import us.mikeandwan.photos.ui.screens.upload.UploadNavKey
import us.mikeandwan.photos.workers.UploadWorker

@HiltViewModel
class MawPhotosAppViewModel
    @Inject
    constructor(
        private val errorRepository: ErrorRepository,
        private val categoryRepository: CategoryRepository,
        private val authService: AuthService,
        private val configRepository: ConfigRepository,
        private val application: Application,
        private val fileStorageRepository: FileStorageRepository,
        private val searchRepository: SearchRepository,
        private val randomMediaRepository: RandomMediaRepository,
        peopleRepository: PeopleRepository,
        clanRepository: ClanRepository,
        placeRepository: PlaceRepository,
    ) : ViewModel() {
        val authenticationStatus = authService.authStatus
        val userStatus = configRepository.userStatus

        // starts Unknown rather than Denied so nothing gated on it is withdrawn before the first
        // read of the token has had a chance to answer
        val faceRecognitionAccess = authService.faceRecognitionAccess
            .stateIn(viewModelScope, WhileSubscribed(5000), ScopeAccess.Unknown)

        // asking again after the user has waved it off would make it a nag, so a dismissal holds
        // for as long as this view model lives - the next launch is the next chance to ask
        private val _reauthorizePromptDismissed = MutableStateFlow(false)

        // a sign in that predates face recognition leaves the people area simply missing, with
        // nothing on screen to say why, so the offer to fix it is made on launch rather than left
        // for the user to find in Settings.  an inactive user is excluded: they are held on a
        // screen of their own, and a wider grant would not change what they can see
        val showReauthorizePrompt = combine(
            faceRecognitionAccess,
            userStatus,
            _reauthorizePromptDismissed,
        ) { access, user, dismissed ->
            access == ScopeAccess.Denied && user !is UserStatus.Inactive && !dismissed
        }.stateIn(viewModelScope, WhileSubscribed(5000), false)

        val years = categoryRepository.getYears()

        // whoever the people screen has already loaded - the rail lists them so a person or a clan
        // can be swapped for another without going back to the grid first.  nothing is fetched
        // here: the people area cannot be reached without the screen that loads it.
        val people = peopleRepository.people
            .map { list -> list.sortedForMenu() }
            .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
        val clans = clanRepository.clans

        // the countries the places screen has already listed, so the rail can offer another branch
        // of the tree from any depth.  nothing is fetched here either: the places area cannot be
        // reached without the screen that lists them.
        val countries = placeRepository.countries

        private val _activeYear = MutableStateFlow(-1)
        val activeYear = _activeYear.asStateFlow()

        // which person, clan or place the rail should mark as current, or null while the listing
        // they are chosen from is what is on screen
        private val _activeFeedSubject = MutableStateFlow<MediaFeedSubject?>(null)
        val activeFeedSubject = _activeFeedSubject.asStateFlow()

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
            .filterIsInstance<ErrorMessage.Display>()
            .map { it }

        val recentSearchTerms = searchRepository
            .getSearchHistory()
            .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

        private var handleIntentJob: Job? = null

        fun setNavArea(area: NavigationArea) {
            _navArea.update { area }
        }

        fun openDrawer() {
            _drawerState.update { DrawerValue.Open }
        }

        fun closeDrawer() {
            _drawerState.update { DrawerValue.Closed }
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
                _topBarState.update { nextState }
            }
        }

        fun setActiveYear(year: Int) {
            _activeYear.update { year }
        }

    fun setActiveFeedSubject(subject: MediaFeedSubject?) {
        _activeFeedSubject.update { subject }
        }

        fun dismissReauthorizePrompt() {
            _reauthorizePromptDismissed.update { true }
        }

        // a fresh login is the only thing that can widen a grant, so the missing authorization is
        // offered as signing in again rather than as a retry.  the prompt is put away either way:
        // a login that succeeds makes it moot, and one that fails should not bounce straight back
        // at the user, who can still reach this from Settings
        fun reauthorize(context: Context) {
            dismissReauthorizePrompt()

            viewModelScope.launch {
                authService.login(context)
            }
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

        private var lastHandledIntent: Intent? = null

        fun handleIntent(intent: Intent?) {
            if (intent == null || intent == lastHandledIntent) {
                return
            }

            lastHandledIntent = intent

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
                    handleIntentJob?.cancel()
                    handleIntentJob = viewModelScope.launch {
                        if (userStatus.value is UserStatus.Unknown &&
                            authenticationStatus.value is AuthStatus.Authorized
                        ) {
                            queryUserStatus()
                        }
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

        // favorites lead, as they do in the grid, so the handful of people looked at most often sit
        // at the top of a list that can run to a few hundred rows
        private fun List<Person>.sortedForMenu(): List<Person> =
            sortedWith(
                compareByDescending<Person> { it.isFavorite }
                    .thenBy { it.name.lowercase() },
            )

        private fun bootstrapAppData() {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val loaded = withTimeoutOrNull(10_000.milliseconds) {
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

            // its own collector so a scope check that has to reach the network cannot hold up the
            // scales, years and categories the app opens on. every status is passed along rather
            // than only an authorized one: losing the session has to withdraw what the last one
            // granted, which is the same call
            viewModelScope.launch {
                authenticationStatus.collect {
                    authService.refreshScopeAccess()
                }
            }
        }
    }
