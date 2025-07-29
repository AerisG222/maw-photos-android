package us.mikeandwan.photos.di

import android.app.Application
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.openid.appauth.AuthorizationService
import us.mikeandwan.photos.R
import us.mikeandwan.photos.authorization.AuthInterceptor
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.database.AuthorizationDao
import us.mikeandwan.photos.domain.AuthorizationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthService(
        application: Application,
        auth0: Auth0,
        credManager: CredentialsManager
    ): AuthService =
        AuthService(application, auth0, credManager)

    @Provides
    @Singleton
    fun provideAuthorizationRepository(authorizationDao: AuthorizationDao): AuthorizationRepository =
        AuthorizationRepository(authorizationDao)

    @Provides
    @Singleton
    fun provideAuthorizationService(application: Application): AuthorizationService =
        AuthorizationService(application)

    @Provides
    @Singleton
    fun provideAuthInterceptor(authService: AuthService, credManager: CredentialsManager): AuthInterceptor =
        AuthInterceptor(authService, credManager)

    @Provides
    @Singleton
    fun provideAuth0(
        application: Application
    ): Auth0 = Auth0.getInstance(
        application.getString(R.string.auth0_client_id),
        application.getString(R.string.auth0_domain)
    )

    @Provides
    @Singleton
    fun provideAuth0AuthenticationClient(
        account: Auth0
    ): AuthenticationAPIClient =
        AuthenticationAPIClient(account)

    @Provides
    @Singleton
    fun provideAuth0SharedPreferencesStorage(
        application: Application
    ): SharedPreferencesStorage =
        SharedPreferencesStorage(application)

    @Provides
    @Singleton
    fun provideAuth0CredentialManager(
        client: AuthenticationAPIClient,
        storage: SharedPreferencesStorage
    ): CredentialsManager =
        CredentialsManager(
            client,
            storage
        )
}
