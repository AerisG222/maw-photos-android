package us.mikeandwan.photos.ui.screens.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.GuardStatus

@HiltViewModel
class UploadViewModel
    @Inject
    constructor(
        authGuard: AuthGuard,
        fileStorageRepository: FileStorageRepository,
    ) : ViewModel() {
        val filesToUpload = fileStorageRepository.pendingUploads

        val isAuthorized = authGuard.status
            .map {
                when (it) {
                    is GuardStatus.Failed -> false
                    else -> true
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), true)

        init {
            viewModelScope.launch {
                fileStorageRepository.refreshPendingUploads()
            }
        }
    }
