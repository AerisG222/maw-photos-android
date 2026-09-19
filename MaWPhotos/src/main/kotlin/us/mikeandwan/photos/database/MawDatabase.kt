package us.mikeandwan.photos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Category::class,
        DeveloperLog::class,
        MediaFile::class,
        Scale::class,
        SearchHistory::class,
        Year::class,
    ],
    version = 23,
)
@TypeConverters(
    Converters::class,
)
abstract class MawDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "us.mikeandwan.photos"
    }

    abstract fun categoryDao(): CategoryDao

    abstract fun developerLogDao(): DeveloperLogDao

    abstract fun scaleDao(): ScaleDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun yearDao(): YearDao
}
