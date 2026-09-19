package us.mikeandwan.photos.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.DeveloperLogDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.ScaleDao
import us.mikeandwan.photos.database.SearchHistoryDao
import us.mikeandwan.photos.database.YearDao
import us.mikeandwan.photos.database.migrations.ALL_MIGRATIONS

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
            ).enableMultiInstanceInvalidation()
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideCategoryDao(mawDatabase: MawDatabase): CategoryDao = mawDatabase.categoryDao()

    @Provides
    fun provideDeveloperLogDao(mawDatabase: MawDatabase): DeveloperLogDao = mawDatabase.developerLogDao()

    @Provides
    fun provideSearchHistoryDao(mawDatabase: MawDatabase): SearchHistoryDao = mawDatabase.searchHistoryDao()

    @Provides
    fun provideScaleDao(mawDatabase: MawDatabase): ScaleDao = mawDatabase.scaleDao()

    @Provides
    fun provideYearDao(mawDatabase: MawDatabase): YearDao = mawDatabase.yearDao()
}
