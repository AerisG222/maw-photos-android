package us.mikeandwan.photos.database.migrations

import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.PersonSort

class MawDatabaseCreateCallback : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        db.execSQL(
            "INSERT INTO category_preference (id, display_type) VALUES (1, '${CategoryDisplayType.Grid}')",
        )
        db.execSQL(
            "INSERT INTO notification_preference (id, do_notify, do_vibrate) VALUES (1, 0, 1)",
        )
        db.execSQL(
            "INSERT INTO media_preference (id, slideshow_interval_seconds, show_face_highlights) VALUES (1, 3, 0)",
        )
        db.execSQL(
            "INSERT INTO people_preference (id, sort_by, show_names, show_media_counts, show_clans, show_category_year, show_category_title) VALUES (1, '${PersonSort.Name}', 1, 1, 1, 1, 1)",
        )
        db.execSQL(
            "INSERT INTO place_preference (id, show_category_year, show_category_title) VALUES (1, 1, 1)",
        )
        db.execSQL(
            "INSERT INTO random_preference (id, slideshow_interval_seconds, show_widget_info) VALUES (1, 3, 1)",
        )
        db.execSQL(
            "INSERT INTO search_preference (id, recent_query_count, display_type) VALUES (1, 20, '${CategoryDisplayType.Grid}')",
        )
    }
}
