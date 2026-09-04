package us.mikeandwan.photos.di

import android.app.Application
import coil3.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.api.ConfigApiClient
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.api.MediaApiClient
import us.mikeandwan.photos.api.PlaceApiClient
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.CategoryPreferenceDao
import us.mikeandwan.photos.database.DeveloperLogDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.MediaPreferenceDao
import us.mikeandwan.photos.database.NotificationPreferenceDao
import us.mikeandwan.photos.database.PeoplePreferenceDao
import us.mikeandwan.photos.database.PlacePreferenceDao
import us.mikeandwan.photos.database.RandomPreferenceDao
import us.mikeandwan.photos.database.ScaleDao
import us.mikeandwan.photos.database.SearchHistoryDao
import us.mikeandwan.photos.database.SearchPreferenceDao
import us.mikeandwan.photos.database.YearDao
import us.mikeandwan.photos.domain.ApiErrorHandler
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaFaceRepository
import us.mikeandwan.photos.domain.MediaFeedRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.NotificationIdRepository
import us.mikeandwan.photos.domain.NotificationPreferenceRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.PlacePreferenceRepository
import us.mikeandwan.photos.domain.PlaceRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.RandomPreferenceRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.services.WidgetRandomPhotoService

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideErrorRepository(developerLogDao: DeveloperLogDao): ErrorRepository = ErrorRepository(developerLogDao)

    @Provides
    @Singleton
    fun provideApiErrorHandler(errorRepository: ErrorRepository): ApiErrorHandler = ApiErrorHandler(errorRepository)

    @Provides
    @Singleton
    fun provideCategoryRepository(
        api: CategoryApiClient,
        db: MawDatabase,
        yearDao: YearDao,
        categoryDao: CategoryDao,
        scaleDao: ScaleDao,
        apiErrorHandler: ApiErrorHandler,
    ): CategoryRepository = CategoryRepository(api, db, yearDao, categoryDao, scaleDao, apiErrorHandler)

    @Provides
    @Singleton
    fun provideConfigRepository(
        api: ConfigApiClient,
        db: MawDatabase,
        scaleDao: ScaleDao,
        apiErrorHandler: ApiErrorHandler,
    ): ConfigRepository = ConfigRepository(api, db, scaleDao, apiErrorHandler)

    @Provides
    @Singleton
    fun provideRandomPhotoRepository(
        api: MediaApiClient,
        randomPreferenceRepository: RandomPreferenceRepository,
        apiErrorHandler: ApiErrorHandler,
    ): RandomMediaRepository = RandomMediaRepository(api, randomPreferenceRepository, apiErrorHandler)

    @Provides
    @Singleton
    fun providePeopleRepository(
        api: FaceApiClient,
        apiErrorHandler: ApiErrorHandler,
    ): PeopleRepository = PeopleRepository(api, apiErrorHandler)

    @Provides
    @Singleton
    fun provideClanRepository(
        api: FaceApiClient,
        apiErrorHandler: ApiErrorHandler,
    ): ClanRepository = ClanRepository(api, apiErrorHandler)

    @Provides
    @Singleton
    fun providePlaceRepository(
        api: PlaceApiClient,
        apiErrorHandler: ApiErrorHandler,
    ): PlaceRepository = PlaceRepository(api, apiErrorHandler)

    // a singleton for the same reason the random feed is one: the grid and the pager are separate
    // screens over the one list of media the user is browsing
    @Provides
    @Singleton
    fun provideMediaFeedRepository(
        faceApi: FaceApiClient,
        placeApi: PlaceApiClient,
        apiErrorHandler: ApiErrorHandler,
    ): MediaFeedRepository = MediaFeedRepository(faceApi, placeApi, apiErrorHandler)

    @Provides
    @Singleton
    fun provideMediaFaceRepository(
        api: MediaApiClient,
        apiErrorHandler: ApiErrorHandler,
    ): MediaFaceRepository = MediaFaceRepository(api, apiErrorHandler)

    @Provides
    @Singleton
    fun provideFileStorageRepository(
        application: Application,
        imageLoader: ImageLoader,
    ): FileStorageRepository = FileStorageRepository(application, imageLoader)

    @Provides
    @Singleton
    fun provideSearchRepository(
        api: CategoryApiClient,
        searchPreferenceRepository: SearchPreferenceRepository,
        searchHistoryDao: SearchHistoryDao,
        apiErrorHandler: ApiErrorHandler,
    ): SearchRepository =
        SearchRepository(
            api,
            searchHistoryDao,
            searchPreferenceRepository,
            apiErrorHandler,
        )

    @Provides
    @Singleton
    fun providesNotificationIdRepository(): NotificationIdRepository = NotificationIdRepository()

    @Provides
    @Singleton
    fun provideCategoryPreferenceRepository(
        categoryPreferenceDao: CategoryPreferenceDao,
    ): CategoryPreferenceRepository = CategoryPreferenceRepository(categoryPreferenceDao)

    @Provides
    @Singleton
    fun provideNotificationPreferenceRepository(
        notificationPreferenceDao: NotificationPreferenceDao,
    ): NotificationPreferenceRepository = NotificationPreferenceRepository(notificationPreferenceDao)

    @Provides
    @Singleton
    fun providePeoplePreferenceRepository(peoplePreferenceDao: PeoplePreferenceDao): PeoplePreferenceRepository =
        PeoplePreferenceRepository(peoplePreferenceDao)

    @Provides
    @Singleton
    fun providePlacePreferenceRepository(placePreferenceDao: PlacePreferenceDao): PlacePreferenceRepository =
        PlacePreferenceRepository(placePreferenceDao)

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

    @Provides
    @Singleton
    fun provideWidgetRandomPhotoService(
        randomMediaRepository: RandomMediaRepository,
        categoryRepository: CategoryRepository,
        randomPreferenceRepository: RandomPreferenceRepository,
        imageLoader: ImageLoader,
        errorRepository: ErrorRepository,
    ): WidgetRandomPhotoService =
        WidgetRandomPhotoService(
            randomMediaRepository,
            categoryRepository,
            randomPreferenceRepository,
            imageLoader,
            errorRepository,
        )
}
