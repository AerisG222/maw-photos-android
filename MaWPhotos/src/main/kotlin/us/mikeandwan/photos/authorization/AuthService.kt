package us.mikeandwan.photos.authorization

import android.app.Application
import android.content.Context
import timber.log.Timber
import us.mikeandwan.photos.R
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.provider.WebAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthService(
    private val application: Application,
    private val auth0: Auth0,
    private val credMgr: CredentialsManager
) {
    private val _authStatus = MutableStateFlow<AuthStatus>(
        if(credMgr.hasValidCredentials())
            AuthStatus.Authorized else AuthStatus.RequiresAuthorization
    )
    val authStatus = _authStatus.asStateFlow()

    init {
        _authStatus.value = AuthStatus.RequiresAuthorization
    }

    suspend fun login(activity: Context) {
        try {
            val credentials = WebAuthProvider
                .login(auth0)
                .withAudience(application.getString(R.string.auth0_audience_api))
                .withScheme(application.getString(R.string.auth0_scheme))
                .withScope(getScopes())
                .await(activity)

            credMgr.saveCredentials(credentials)
            _authStatus.value = AuthStatus.Authorized

            Timber.d("Successfully logged in with Auth0")
        } catch (e: AuthenticationException) {
            _authStatus.value = AuthStatus.RequiresAuthorization
            Timber.e(e, "Error trying to login with Auth0")
        }
    }

    suspend fun logout(activity: Context) {
        try {
            WebAuthProvider
                .logout(auth0)
                .withScheme(application.getString(R.string.auth0_scheme))
                .await(activity)
            credMgr.clearCredentials()
        } catch(e: AuthenticationException) {
            Timber.e(e, "Error trying to logout from Auth0")
        }
    }

    fun updateStatus(newStatus: AuthStatus) {
        Timber.d("Updating auth status to $newStatus")
        _authStatus.value = newStatus
    }

    fun getScopes(): String {
        return arrayOf(
            "openid",
            "email",
            "profile",
            "offline_access",
            "${application.getString(R.string.auth0_audience_api)}/media:read",
            "${application.getString(R.string.auth0_audience_api)}/media:write",
            "${application.getString(R.string.auth0_audience_api)}/comments:read",
            "${application.getString(R.string.auth0_audience_api)}/comments:write"
        ).joinToString(" ")
    }
}
