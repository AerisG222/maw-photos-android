package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE random_preference
                ADD COLUMN show_widget_info INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )
    }
}
