package us.mikeandwan.photos.di

import android.app.Application
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import us.mikeandwan.photos.Constants
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.authorization.AuthInterceptor
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.api.ConfigApiClient
import us.mikeandwan.photos.api.UploadApiClient

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
    fun provideImageLoader(
        application: Application,
        httpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(application)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            httpClient
                        }
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(application, 0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(application.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideCategoryApiClient(retrofit: Retrofit): CategoryApiClient =
        CategoryApiClient(retrofit)

    @Provides
    @Singleton
    fun provideConfigApiClient(retrofit: Retrofit): ConfigApiClient =
        ConfigApiClient(retrofit)

    @Provides
    @Singleton
    fun provideMediaApiClient(retrofit: Retrofit): MediaApiClient =
        MediaApiClient(retrofit)

    @Provides
    @Singleton
    fun provideUploadApiClient(retrofit: Retrofit): UploadApiClient =
        UploadApiClient(retrofit)
}
