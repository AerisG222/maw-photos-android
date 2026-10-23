package us.mikeandwan.photos.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        DataStore
            .Builder(
                storage =
                    FileStorage(
                        serializer = UserPreferencesSerializer,
                        produceFile = { application.dataStoreFile(USER_PREFERENCES_FILE) },
                    ),
                // must outlive every collector, so it lives as long as the process (never a viewModelScope)
                context = Dispatchers.IO + SupervisorJob(),
            )
            // settings that cannot be read are not worth refusing to start over
            .setCorruptionHandler(ReplaceFileCorruptionHandler { UserPreferencesSerializer.defaultValue })
            .addMigrations(listOf(RoomPreferencesImport(mawDatabase)))
            .build()
}
