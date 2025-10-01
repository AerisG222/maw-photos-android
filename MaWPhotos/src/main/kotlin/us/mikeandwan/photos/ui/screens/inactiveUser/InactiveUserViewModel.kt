package us.mikeandwan.photos.ui.screens.inactiveUser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.models.UserStatus
import javax.inject.Inject

@HiltViewModel
class InactiveUserViewModel @Inject constructor(
    private val authService: AuthService,
    private val configRepository: ConfigRepository,
    private val errorRepository: ErrorRepository,
): ViewModel() {
    val userStatus = configRepository.userStatus

    fun queryUserStatus() {
        viewModelScope.launch {
            configRepository.getUserStatus()

            if(configRepository.userStatus.value is UserStatus.Inactive) {
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
