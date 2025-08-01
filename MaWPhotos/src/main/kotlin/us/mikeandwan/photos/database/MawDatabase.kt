package us.mikeandwan.photos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryPreference::class,
        MediaPreference::class,
        NotificationPreference::class,
        PhotoCategory::class,
        RandomPreference::class,
        SearchHistory::class,
        SearchPreference::class
    ],
    version = 6
)
@TypeConverters(
    Converters::class
)
abstract class MawDatabase : RoomDatabase() {
    abstract fun categoryPreferenceDao(): CategoryPreferenceDao
    abstract fun mediaCategoryDao(): MediaCategoryDao
    abstract fun mediaPreferenceDao(): MediaPreferenceDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun photoCategoryDao(): PhotoCategoryDao
    abstract fun randomPreferenceDao(): RandomPreferenceDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun searchPreferenceDao(): SearchPreferenceDao
}
