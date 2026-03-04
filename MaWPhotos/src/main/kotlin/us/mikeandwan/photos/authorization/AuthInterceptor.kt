package us.mikeandwan.photos.authorization

import android.app.Application
import com.auth0.android.authentication.storage.CredentialsManager
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import us.mikeandwan.photos.R

class AuthInterceptor(
    private val application: Application,
    private val credManager: CredentialsManager,
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val srcRequest = chain.request()

        // Only send access token to the API that requires it
        val needsToken = srcRequest.url
            .toString()
            .startsWith(application.getString(R.string.auth0_audience_api), true)

        if (needsToken) {
            val accessToken = runBlocking {
                try {
                    // awaitCredentials() returns the current token if valid,
                    // or refreshes it if it has expired.
                    credManager.awaitCredentials().accessToken
                } catch (e: Exception) {
                    null
                }
            }

            if (accessToken != null) {
                val request = srcRequest
                    .newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                return chain.proceed(request)
            }
        }

        return chain.proceed(srcRequest)
    }
}
