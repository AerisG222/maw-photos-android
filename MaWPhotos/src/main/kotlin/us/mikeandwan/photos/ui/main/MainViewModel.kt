package us.mikeandwan.photos.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DrawerValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.models.ErrorMessage
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.controls.topbar.TopBarState
import us.mikeandwan.photos.workers.UploadWorker
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    errorRepository: ErrorRepository,
    categoryRepository: CategoryRepository,
    private val imageLoader: ImageLoader,
    private val application: Application,
    private val fileStorageRepository: FileStorageRepository,
    private val searchRepository: SearchRepository,
    private val randomMediaRepository: RandomMediaRepository
): ViewModel() {
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

    private val _signalNavigate = MutableStateFlow<Any?>(null)
    val signalNavigate = _signalNavigate.asStateFlow()

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

    fun navigate(destination: Any) {
        _signalNavigate.value = destination
    }

    fun navigateAndCloseDrawer(destination: Any) {
        closeDrawer()
        navigate(destination)
    }

    fun updateTopBar(nextState: TopBarState) {
        _topBarState.value = nextState
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

    fun handleSendSingle(intent: Intent) {
        val mediaUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )

        if(mediaUri != null) {
            enqueueUpload(mediaUri)
        }
    }

    fun handleSendMultiple(intent: Intent) {
        val mediaUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )

        if(mediaUris != null) {
            enqueueUpload(*mediaUris.toTypedArray())
        }
    }

    private fun enqueueUpload(vararg mediaUri: Uri) {
        viewModelScope.launch {
            mediaUri.forEach {
                val file = saveUploadFile(it)

                if (file != null) {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()

                    val data = workDataOf(
                        UploadWorker.KEY_FILENAME to file.path
                    )

                    val work = OneTimeWorkRequestBuilder<UploadWorker>()
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            1,
                            TimeUnit.MINUTES
                        )
                        .setConstraints(constraints)
                        .setInputData(data)
                        .build()

                    val workManager = WorkManager.getInstance(application)

                    workManager.enqueueUniqueWork(
                        "upload ${file.path}",
                        ExistingWorkPolicy.REPLACE,
                        work
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

    private suspend fun saveUploadFile(mediaUri: Uri): File? {
        return fileStorageRepository.saveFileToUpload(mediaUri)
    }

    init {
        SingletonImageLoader.setSafe { imageLoader }

        viewModelScope.launch {
            fileStorageRepository.refreshPendingUploads()
            clearFileCache()
        }
    }
}
