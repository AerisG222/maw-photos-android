package us.mikeandwan.photos.authorization

import com.auth0.android.authentication.storage.CredentialsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator(
    private val authService: AuthService,
    private val credManager: CredentialsManager,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // Only retry once per request to avoid infinite 401 loops
        if (response.priorResponse != null) {
            return null
        }

        Timber.d("Authenticator: 401 detected for ${response.request.url}. Attempting token refresh.")

        return runBlocking {
            try {
                // awaitCredentials() is thread-safe and handles the refresh logic internally.
                val credentials = credManager.awaitCredentials()
                val newAccessToken = credentials.accessToken

                // If the token in the failed request is different from the fresh one,
                // another thread already refreshed it. We can just retry with the fresh one.
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                if (requestToken == newAccessToken) {
                    // Refresh happened but we still got a 401. Token might be invalid or
                    // user might have been de-authorized on the server.
                    Timber.w("Authenticator: Refresh provided the same token. De-authorizing.")
                    authService.updateStatus(AuthStatus.RequiresAuthorization)
                    return@runBlocking null
                }

                Timber.d("Authenticator: Retrying ${response.request.url} with fresh token.")
                response.request
                    .newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } catch (e: Exception) {
                Timber.e(e, "Authenticator: Failed to refresh token.")
                authService.updateStatus(AuthStatus.RequiresAuthorization)
                null
            }
        }
    }
}
