package us.mikeandwan.photos.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.domain.CategoryRepository
import javax.inject.Inject

sealed class LoginState {
    data object Unknown : LoginState()
    data object Authorized : LoginState()
    data object NotAuthorized : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
    private val categoryRepository: CategoryRepository
): ViewModel() {
    val state = authService.authStatus
        .map {
            when(it) {
                is AuthStatus.Authorized -> LoginState.Authorized
                is AuthStatus.RequiresAuthorization -> LoginState.NotAuthorized

                // when in process, return the notauthorized state, as this will catch the case where the user
                // starts authentication but does not complete that action (closing browser tab / going back / etc)
                is AuthStatus.LoginInProcess -> LoginState.NotAuthorized
            }
        }
        .stateIn(viewModelScope, WhileSubscribed(5000), LoginState.Unknown)

    suspend fun refreshCategories() {
        categoryRepository
            .getNewCategories()
            .collect { }
    }

    fun login(activity: Context) {
        viewModelScope.launch {
            authService.login(activity)
        }
    }
}
