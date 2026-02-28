package us.mikeandwan.photos.ui.screens.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus

data class UploadUiState(
    val filesToUpload: List<File> = emptyList(),
    val isAuthorized: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class UploadViewModel
    @Inject
    constructor(
        authGuard: AuthGuard,
        private val fileStorageRepository: FileStorageRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UploadUiState())
        val uiState = _uiState.asStateFlow()

        init {
            combine(
                fileStorageRepository.pendingUploads,
                authGuard.status.map { it !is GuardStatus.Failed }
            ) { files, isAuthorized ->
                UploadUiState(
                    filesToUpload = files,
                    isAuthorized = isAuthorized,
                    isLoading = false
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)

            viewModelScope.launch {
                fileStorageRepository.refreshPendingUploads()
            }
        }
    }
