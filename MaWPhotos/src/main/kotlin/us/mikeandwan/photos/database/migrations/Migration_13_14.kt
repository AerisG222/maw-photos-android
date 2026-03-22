package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE media_preference
                ADD COLUMN show_media_type_indicator INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )
    }
}
