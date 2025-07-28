package us.mikeandwan.photos.di

import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import us.mikeandwan.photos.Constants
import us.mikeandwan.photos.api.PhotoApiClient
import us.mikeandwan.photos.authorization.AuthInterceptor
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.api.SearchApiClient
import us.mikeandwan.photos.api.UploadApiClient
import us.mikeandwan.photos.api.VideoApiClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        if(BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC

            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(
        okHttpClient: OkHttpClient
    ): HttpDataSource.Factory =
        OkHttpDataSource.Factory(okHttpClient)

    @Provides
    @Singleton
    fun provideRetrofit(httpClient: OkHttpClient): Retrofit {
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .addConverterFactory(
                json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .client(httpClient)
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoApiClient(retrofit: Retrofit): PhotoApiClient =
        PhotoApiClient(retrofit)

    @Provides
    @Singleton
    fun provideSearchApiClient(retrofit: Retrofit): SearchApiClient =
        SearchApiClient(retrofit)

    @Provides
    @Singleton
    fun provideUploadApiClient(retrofit: Retrofit): UploadApiClient =
        UploadApiClient(retrofit)

    @Provides
    @Singleton
    fun provideVideoApiClient(retrofit: Retrofit): VideoApiClient =
        VideoApiClient(retrofit)
}
