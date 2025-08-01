package us.mikeandwan.photos.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.mikeandwan.photos.database.*
import us.mikeandwan.photos.database.migrations.MIGRATION_1_2
import us.mikeandwan.photos.database.migrations.MIGRATION_2_3
import us.mikeandwan.photos.database.migrations.MIGRATION_3_4
import us.mikeandwan.photos.database.migrations.MIGRATION_4_5
import us.mikeandwan.photos.database.migrations.MIGRATION_5_6
import us.mikeandwan.photos.database.migrations.MawDatabaseCreateCallback
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMawDatabase(application: Application): MawDatabase =
        Room.databaseBuilder(
            application,
            MawDatabase::class.java,
            "us.mikeandwan.photos"
        )
        .addCallback(MawDatabaseCreateCallback())
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .build()

    @Provides
    @Singleton
    fun provideCategoryPreferenceDao(mawDatabase: MawDatabase): CategoryPreferenceDao =
        mawDatabase.categoryPreferenceDao()

    @Provides
    @Singleton
    fun provideMediaCategoryDao(mawDatabase: MawDatabase): MediaCategoryDao =
        mawDatabase.mediaCategoryDao()

    @Provides
    @Singleton
    fun provideNotificationPreferenceDao(mawDatabase: MawDatabase): NotificationPreferenceDao =
        mawDatabase.notificationPreferenceDao()

    @Provides
    @Singleton
    fun providePhotoCategoryDao(mawDatabase: MawDatabase): PhotoCategoryDao =
        mawDatabase.photoCategoryDao()

    @Provides
    @Singleton
    fun providePhotoPreferenceDao(mawDatabase: MawDatabase): MediaPreferenceDao =
        mawDatabase.mediaPreferenceDao()

    @Provides
    @Singleton
    fun provideRandomPreferenceDao(mawDatabase: MawDatabase): RandomPreferenceDao =
        mawDatabase.randomPreferenceDao()

    @Provides
    @Singleton
    fun provideSearchHistoryDao(mawDatabase: MawDatabase): SearchHistoryDao =
        mawDatabase.searchHistoryDao()

    @Provides
    @Singleton
    fun provideSearchPreferenceDao(mawDatabase: MawDatabase): SearchPreferenceDao =
        mawDatabase.searchPreferenceDao()
}
