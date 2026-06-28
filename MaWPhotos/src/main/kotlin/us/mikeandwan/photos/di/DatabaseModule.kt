package us.mikeandwan.photos.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.CategoryPreferenceDao
import us.mikeandwan.photos.database.DeveloperLogDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.MediaPreferenceDao
import us.mikeandwan.photos.database.NotificationPreferenceDao
import us.mikeandwan.photos.database.RandomPreferenceDao
import us.mikeandwan.photos.database.ScaleDao
import us.mikeandwan.photos.database.SearchHistoryDao
import us.mikeandwan.photos.database.SearchPreferenceDao
import us.mikeandwan.photos.database.YearDao
import us.mikeandwan.photos.database.migrations.ALL_MIGRATIONS
import us.mikeandwan.photos.database.migrations.MawDatabaseCreateCallback

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMawDatabase(application: Application): MawDatabase =
        Room
            .databaseBuilder(
                application,
                MawDatabase::class.java,
                MawDatabase.DATABASE_NAME,
            ).addCallback(MawDatabaseCreateCallback())
            .enableMultiInstanceInvalidation()
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideCategoryPreferenceDao(mawDatabase: MawDatabase): CategoryPreferenceDao =
        mawDatabase.categoryPreferenceDao()

    @Provides
    fun provideCategoryDao(mawDatabase: MawDatabase): CategoryDao = mawDatabase.categoryDao()

    @Provides
    fun provideDeveloperLogDao(mawDatabase: MawDatabase): DeveloperLogDao = mawDatabase.developerLogDao()

    @Provides
    fun provideNotificationPreferenceDao(mawDatabase: MawDatabase): NotificationPreferenceDao =
        mawDatabase.notificationPreferenceDao()

    @Provides
    fun providePhotoPreferenceDao(mawDatabase: MawDatabase): MediaPreferenceDao = mawDatabase.mediaPreferenceDao()

    @Provides
    fun provideRandomPreferenceDao(mawDatabase: MawDatabase): RandomPreferenceDao = mawDatabase.randomPreferenceDao()

    @Provides
    fun provideSearchHistoryDao(mawDatabase: MawDatabase): SearchHistoryDao = mawDatabase.searchHistoryDao()

    @Provides
    fun provideSearchPreferenceDao(mawDatabase: MawDatabase): SearchPreferenceDao = mawDatabase.searchPreferenceDao()

    @Provides
    fun provideScaleDao(mawDatabase: MawDatabase): ScaleDao = mawDatabase.scaleDao()

    @Provides
    fun provideYearDao(mawDatabase: MawDatabase): YearDao = mawDatabase.yearDao()
}
