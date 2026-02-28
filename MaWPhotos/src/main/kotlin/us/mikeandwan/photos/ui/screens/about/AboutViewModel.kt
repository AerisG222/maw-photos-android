package us.mikeandwan.photos.ui.screens.about

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.R

data class AboutUiState(
    val version: String = "",
    val history: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        private val application: Application,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(
            AboutUiState(version = "v${BuildConfig.VERSION_NAME}")
        )
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch(Dispatchers.IO) {
                val history = application.resources
                    .openRawResource(R.raw.release_notes)
                    .bufferedReader()
                    .use { it.readText() }

                _uiState.update {
                    it.copy(
                        history = history,
                        isLoading = false,
                    )
                }
            }
        }
    }
