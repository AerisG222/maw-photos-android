package us.mikeandwan.photos.database.migrations

import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize

class MawDatabaseCreateCallback : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        db.execSQL(
            "INSERT INTO category_preference (id, display_type, grid_thumbnail_size) VALUES (1, '${CategoryDisplayType.Grid}', '${GridThumbnailSize.Medium}')",
        )
        db.execSQL("INSERT INTO notification_preference (id, do_notify, do_vibrate) VALUES (1, 0, 1)")
        db.execSQL(
            "INSERT INTO media_preference (id, slideshow_interval_seconds, grid_thumbnail_size) VALUES (1, 3, '${GridThumbnailSize.Medium}')",
        )
        db.execSQL(
            "INSERT INTO random_preference (id, slideshow_interval_seconds, grid_thumbnail_size) VALUES (1, 3, '${GridThumbnailSize.Medium}')",
        )
        db.execSQL(
            "INSERT INTO search_preference (id, recent_query_count, display_type, grid_thumbnail_size) VALUES (1, 20, '${CategoryDisplayType.Grid}', '${GridThumbnailSize.Medium}')",
        )
    }
}
