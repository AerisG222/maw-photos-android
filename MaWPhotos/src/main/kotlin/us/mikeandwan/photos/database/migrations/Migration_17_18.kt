package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18 = object : Migration(17, 18) {
    private val tables = listOf(
        "category_preference",
        "media_preference",
        "random_preference",
        "search_preference",
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        tables.forEach { table ->
            db.execSQL(
                """
                ALTER TABLE $table
                    ADD COLUMN show_favorite_indicator INTEGER NOT NULL DEFAULT 1
                """.trimIndent(),
            )
        }
    }
}
