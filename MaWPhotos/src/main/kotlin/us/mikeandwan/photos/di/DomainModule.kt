package us.mikeandwan.photos.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.database.*
import us.mikeandwan.photos.domain.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideErrorRepository(): ErrorRepository =
        ErrorRepository()

    @Provides
    @Singleton
    fun provideApiErrorHandler(
        errorRepository: ErrorRepository
    ): ApiErrorHandler =
        ApiErrorHandler(errorRepository)

    @Provides
    @Singleton
    fun provideCategoryRepository(
        api: CategoryApiClient,
        db: MawDatabase,
        yearDao: YearDao,
        categoryDao: CategoryDao,
        scaleDao: ScaleDao,
        apiErrorHandler: ApiErrorHandler
    ): CategoryRepository =
        CategoryRepository(api, db, yearDao, categoryDao, scaleDao, apiErrorHandler)

    @Provides
    @Singleton
    fun provideRandomPhotoRepository(
        api: MediaApiClient,
        randomPreferenceRepository: RandomPreferenceRepository,
        apiErrorHandler: ApiErrorHandler
    ): RandomPhotoRepository =
        RandomPhotoRepository(api, randomPreferenceRepository, apiErrorHandler)

    @Provides
    @Singleton
    fun provideFileStorageRepository(
        application: Application
    ): FileStorageRepository =
        FileStorageRepository(application)

    @Provides
    @Singleton
    fun provideSearchRepository(
        api: CategoryApiClient,
        searchPreferenceRepository: SearchPreferenceRepository,
        searchHistoryDao: SearchHistoryDao,
        apiErrorHandler: ApiErrorHandler
    ): SearchRepository =
        SearchRepository(
            api,
            searchHistoryDao,
            searchPreferenceRepository,
            apiErrorHandler
        )

    @Provides
    @Singleton
    fun provideCategoryPreferenceRepository(categoryPreferenceDao: CategoryPreferenceDao): CategoryPreferenceRepository =
        CategoryPreferenceRepository(categoryPreferenceDao)

    @Provides
    @Singleton
    fun provideNotificationPreferenceRepository(notificationPreferenceDao: NotificationPreferenceDao): NotificationPreferenceRepository =
        NotificationPreferenceRepository(notificationPreferenceDao)

    @Provides
    @Singleton
    fun providePhotoPreferenceRepository(photoPreferenceDao: MediaPreferenceDao): MediaPreferenceRepository =
        MediaPreferenceRepository(photoPreferenceDao)

    @Provides
    @Singleton
    fun provideRandomPreferenceRepository(randomPreferenceDao: RandomPreferenceDao): RandomPreferenceRepository =
        RandomPreferenceRepository(randomPreferenceDao)

    @Provides
    @Singleton
    fun provideSearchPreferenceRepository(searchPreferenceDao: SearchPreferenceDao): SearchPreferenceRepository =
        SearchPreferenceRepository(searchPreferenceDao)
}
