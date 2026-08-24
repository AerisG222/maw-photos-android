package us.mikeandwan.photos.authorization

import android.app.Application
import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import timber.log.Timber
import us.mikeandwan.photos.R

class AuthService(
    private val application: Application,
    private val auth0: Auth0,
    private val credMgr: CredentialsManager,
) {
    private val _authStatus = MutableStateFlow(
        if (credMgr.hasValidCredentials()) {
            AuthStatus.Authorized
        } else {
            AuthStatus.RequiresAuthorization
        },
    )
    val authStatus = _authStatus.asStateFlow()

    // null until something has been read, which is why this is not exposed directly - see
    // ScopeAccess for why "cannot tell" has to stay distinct from "holds nothing"
    private val _grantedScopes = MutableStateFlow<Set<String>?>(null)

    private val faceRecognitionScope by lazy { qualify(ApiScopes.FACE_RECOGNITION_READ) }

    private var attemptedScopeUpgrade = false

    /**
     * Whether the credentials in hand let the caller read people, faces and face crops.
     *
     * Anything face related is gated on this rather than tried and handled on failure: without the
     * scope the API answers 403 for every one of those calls, including the images, and a screen
     * full of broken tiles is a worse way to say "you need to sign in again" than not offering the
     * screen.
     */
    val faceRecognitionAccess = _grantedScopes.map { granted ->
        when {
            granted == null -> ScopeAccess.Unknown
            faceRecognitionScope in granted -> ScopeAccess.Granted
            else -> ScopeAccess.Denied
        }
    }

    // force login screen on app start for testing
    //    init {
    //        _authStatus.update { AuthStatus.RequiresAuthorization }
    //    }

    suspend fun login(activity: Context) {
        try {
            val credentials = WebAuthProvider
                .login(auth0)
                .withAudience(application.getString(R.string.auth0_audience_api))
                .withScheme(application.getString(R.string.auth0_scheme))
                .withScope(getScopes())
                .await(activity)

            credMgr.saveCredentials(credentials)
            _authStatus.update { AuthStatus.Authorized }

            // the token just handed over says what it grants, so this costs no round trip. a fresh
            // login is also the one thing that can widen a grant, so the upgrade attempt made
            // against the old credentials is allowed to happen again
            attemptedScopeUpgrade = false
            _grantedScopes.update { decodeGrantedScopes(credentials.accessToken) }

            Timber.d("Successfully logged in with Auth0")
        } catch (e: AuthenticationException) {
            _authStatus.update { AuthStatus.RequiresAuthorization }
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
            _authStatus.update { AuthStatus.RequiresAuthorization }
            _grantedScopes.update { null }
        } catch (e: AuthenticationException) {
            Timber.e(e, "Error trying to logout from Auth0")
        }
    }

    fun updateStatus(newStatus: AuthStatus) {
        Timber.d("Updating auth status to $newStatus")
        _authStatus.update { newStatus }
    }

    /**
     * Re-reads which scopes the credentials in hand actually carry.
     *
     * Credentials saved before face recognition was added carry the scopes asked for at the time,
     * and a refresh cannot widen a grant, so this also makes one attempt to renew with the current
     * scope set. Auth0 grants that only when the client is already entitled to it; when it does
     * not, the user has to sign in again, which is what [faceRecognitionAccess] going to
     * [ScopeAccess.Denied] tells the UI to offer.
     */
    suspend fun refreshScopeAccess() {
        if (_authStatus.value !is AuthStatus.Authorized) {
            _grantedScopes.update { null }

            return
        }

        val granted = readGrantedScopes { credMgr.awaitCredentials() }

        if (granted == null || faceRecognitionScope in granted) {
            _grantedScopes.update { granted }

            return
        }

        // one attempt per process: passing a scope makes the manager compare it against the stored
        // one and renew whenever the two differ, and a grant that will not widen never will, so a
        // second call would be a network round trip with a foregone conclusion
        if (attemptedScopeUpgrade) {
            _grantedScopes.update { granted }

            return
        }

        attemptedScopeUpgrade = true

        Timber.d("Access token predates a requested scope; attempting to renew with the current set")

        _grantedScopes.update { readGrantedScopes { credMgr.awaitCredentials(getScopes(), 0) } ?: granted }
    }

    private suspend fun readGrantedScopes(fetch: suspend () -> Credentials): Set<String>? =
        try {
            decodeGrantedScopes(fetch().accessToken)
        } catch (t: Throwable) {
            // an unreachable API or a failed renewal says nothing about what was granted, so this
            // stays unknown rather than becoming a denial the UI would ask the user to fix
            Timber.w(t, "Unable to read credentials while checking granted scopes")

            null
        }

    fun getScopes(): String =
        arrayOf(
            "openid",
            "email",
            "profile",
            "offline_access",
            qualify(ApiScopes.MEDIA_READ),
            qualify(ApiScopes.MEDIA_WRITE),
            qualify(ApiScopes.COMMENTS_READ),
            qualify(ApiScopes.COMMENTS_WRITE),
            qualify(ApiScopes.FACE_RECOGNITION_READ),
        ).joinToString(" ")

    // Auth0 issues scopes for a custom API prefixed with the API identifier, and the API requires
    // them in that form - see ApiScopes.Qualify in maw-media
    fun qualify(scope: String) = "${application.getString(R.string.auth0_audience_api)}/$scope"
}
