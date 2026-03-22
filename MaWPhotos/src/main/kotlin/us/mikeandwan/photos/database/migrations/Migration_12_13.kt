package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE category_preference
                ADD COLUMN show_media_type_indicator INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )

        db.execSQL(
            """
            ALTER TABLE random_preference
                ADD COLUMN show_media_type_indicator INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )

        db.execSQL(
            """
            ALTER TABLE search_preference
                ADD COLUMN show_media_type_indicator INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )
    }
}
