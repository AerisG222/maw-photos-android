package us.mikeandwan.photos.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.AuthStatus

data class LoginUiState(
    val isAuthorized: Boolean = false,
)

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authService: AuthService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState = _uiState.asStateFlow()

        init {
            authService.authStatus
                .onEach { status ->
                    _uiState.update {
                        it.copy(isAuthorized = status is AuthStatus.Authorized)
                    }
                }.launchIn(viewModelScope)
        }

        fun login(activity: Context) {
            viewModelScope.launch {
                authService.login(activity)
            }
        }
    }
