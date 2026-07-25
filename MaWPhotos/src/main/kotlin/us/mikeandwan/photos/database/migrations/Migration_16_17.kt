package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// GridThumbnailSize lost ExtraSmall and Unspecified, and CategoryDisplayType lost Unspecified.
// Room resolves enums by name, so any preference still holding one of those values would fail to
// read - move the retired sizes onto a supported one and treat unset as the largest size.
val MIGRATION_16_17 = object : Migration(16, 17) {
    private val sizeTables = listOf(
        "category_preference",
        "media_preference",
        "random_preference",
        "search_preference",
    )

    private val displayTypeTables = listOf(
        "category_preference",
        "search_preference",
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        sizeTables.forEach { table ->
            db.execSQL(
                """
                UPDATE $table
                SET grid_thumbnail_size = 'Small'
                WHERE grid_thumbnail_size = 'ExtraSmall'
                """.trimIndent(),
            )

            db.execSQL(
                """
                UPDATE $table
                SET grid_thumbnail_size = 'Medium'
                WHERE grid_thumbnail_size = 'Unspecified'
                """.trimIndent(),
            )
        }

        displayTypeTables.forEach { table ->
            db.execSQL(
                """
                UPDATE $table
                SET display_type = 'Grid'
                WHERE display_type = 'Unspecified'
                """.trimIndent(),
            )
        }
    }
}
