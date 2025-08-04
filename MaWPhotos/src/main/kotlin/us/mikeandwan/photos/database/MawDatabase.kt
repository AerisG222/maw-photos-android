package us.mikeandwan.photos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Category::class,
        CategoryPreference::class,
        MediaFile::class,
        MediaPreference::class,
        NotificationPreference::class,
        RandomPreference::class,
        Scale::class,
        SearchHistory::class,
        SearchPreference::class,
        Year::class
    ],
    version = 9
)
@TypeConverters(
    Converters::class
)
abstract class MawDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryPreferenceDao(): CategoryPreferenceDao
    abstract fun mediaPreferenceDao(): MediaPreferenceDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun randomPreferenceDao(): RandomPreferenceDao
    abstract fun scaleDao(): ScaleDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun searchPreferenceDao(): SearchPreferenceDao
    abstract fun yearDao(): YearDao
}
