package us.mikeandwan.photos.ui.screens.inactiveUser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.models.UserStatus

data class InactiveUserUiState(
    val userStatus: UserStatus = UserStatus.Unknown,
    val isLoading: Boolean = false
)

@HiltViewModel
class InactiveUserViewModel
    @Inject
    constructor(
        private val authService: AuthService,
        private val configRepository: ConfigRepository,
        private val errorRepository: ErrorRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(InactiveUserUiState())
        val uiState = _uiState.asStateFlow()

        init {
            configRepository.userStatus
                .onEach { status ->
                    _uiState.update { it.copy(userStatus = status) }
                }.launchIn(viewModelScope)
        }

        fun queryUserStatus() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                configRepository.getUserStatus()
                _uiState.update { it.copy(isLoading = false) }

                if (configRepository.userStatus.value is UserStatus.Inactive) {
                    errorRepository.showThenClearError("Sorry, your account is still inactive.")
                }
            }
        }

        fun logout(context: Context) {
            viewModelScope.launch {
                authService.logout(context)
                configRepository.clearUserStatus()
            }
        }
    }
