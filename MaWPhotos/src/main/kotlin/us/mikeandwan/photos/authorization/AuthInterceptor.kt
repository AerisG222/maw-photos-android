package us.mikeandwan.photos.authorization

import com.auth0.android.authentication.storage.CredentialsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class AuthInterceptor(
    private val authService: AuthService,
    private val credManager: CredentialsManager
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val srcRequest = chain.request()
        var accessToken: String? = null

        runBlocking {
            try {
                val credentials = credManager.awaitCredentials()
                accessToken = credentials.accessToken
            }
            catch (e: Exception) {
                authService.updateStatus(AuthStatus.RequiresAuthorization)
                Timber.e(e, "Error trying to get credentials")
            }
        }

        if(accessToken != null) {
            val request = srcRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()

            return chain.proceed(request)
        }

        return chain.proceed(srcRequest)
    }
}
