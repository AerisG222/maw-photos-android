package us.mikeandwan.photos.ui.screens.about

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.R

sealed class AboutState {
    data object Loading : AboutState()

    data class Loaded(
        val version: String,
        val history: String,
    )
}

@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        private val application: Application,
    ) : ViewModel() {
        private val version = "v${BuildConfig.VERSION_NAME}"
        private val _history = MutableStateFlow("")

        val state = _history
            .map { history ->
                if (history.isEmpty()) {
                    AboutState.Loading
                } else {
                    AboutState.Loaded(version, history)
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), AboutState.Loading)

        init {
            viewModelScope.launch(Dispatchers.IO) {
                _history.value = application.resources
                    .openRawResource(R.raw.release_notes)
                    .bufferedReader()
                    .use { it.readText() }
            }
        }
    }
