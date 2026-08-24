package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.PersonSort

// browsing by person: its own preferences, plus the one setting that governs whether faces are
// marked over media wherever it is shown
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `people_preference` (
                `id` INTEGER NOT NULL,
                `sort_by` TEXT NOT NULL DEFAULT 'Name',
                `grid_thumbnail_size` TEXT NOT NULL,
                `show_names` INTEGER NOT NULL DEFAULT 1,
                `show_media_counts` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        // the row every read of this table expects.  a fresh install gets it from
        // MawDatabaseCreateCallback instead, which is why this one is only for upgrades.
        db.execSQL(
            """
            INSERT INTO people_preference (id, sort_by, grid_thumbnail_size, show_names, show_media_counts)
                VALUES (1, '${PersonSort.Name}', '${GridThumbnailSize.Medium}', 1, 1)
            """.trimIndent(),
        )

        db.execSQL(
            """
            ALTER TABLE media_preference
                ADD COLUMN show_face_highlights INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
    }
}
