package us.mikeandwan.photos.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.datastore.RoomPreferencesImport
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.UserPreferencesSerializer

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    private const val USER_PREFERENCES_FILE = "user_preferences.json"

    // a single instance for the whole process: datastore refuses to have two open on the same file
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        application: Application,
        mawDatabase: MawDatabase,
    ): DataStore<UserPreferences> =
        DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            // settings that cannot be read are not worth refusing to start over
            corruptionHandler = ReplaceFileCorruptionHandler { UserPreferencesSerializer.defaultValue },
            migrations = listOf(RoomPreferencesImport(mawDatabase)),
            produceFile = { application.dataStoreFile(USER_PREFERENCES_FILE) },
        )
}
