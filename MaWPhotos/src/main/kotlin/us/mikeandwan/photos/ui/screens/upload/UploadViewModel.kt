package us.mikeandwan.photos.ui.screens.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.FileStorageRepository

data class UploadUiState(
    val filesToUpload: List<File> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class UploadViewModel
    @Inject
    constructor(
        private val fileStorageRepository: FileStorageRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UploadUiState())
        val uiState = _uiState.asStateFlow()

        init {
            fileStorageRepository.pendingUploads
                .onEach { files ->
                    _uiState.update {
                        it.copy(
                            filesToUpload = files,
                            isLoading = false,
                        )
                    }
                }.launchIn(viewModelScope)

            viewModelScope.launch {
                fileStorageRepository.refreshPendingUploads()
            }
        }
    }
