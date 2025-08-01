package us.mikeandwan.photos.authorization

import android.app.Application
import com.auth0.android.authentication.storage.CredentialsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import us.mikeandwan.photos.R
import java.io.IOException

class AuthInterceptor(
    private val application: Application,
    private val authService: AuthService,
    private val credManager: CredentialsManager
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val srcRequest = chain.request()

        // only send access token to the API that requires it
        val needsToken = srcRequest.url.toString()
            .startsWith(application.getString(R.string.auth0_audience_api), true)

        if(needsToken)
        {
            var accessToken: String? = null

            runBlocking {
                try {
                    val credentials = credManager.awaitCredentials()
                    accessToken = credentials.accessToken
                    Timber.i("Attempting to get credentials from Auth0: ${srcRequest.url}" )
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
        }

        return chain.proceed(srcRequest)
    }
}
