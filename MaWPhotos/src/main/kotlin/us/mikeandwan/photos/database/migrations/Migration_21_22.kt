package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// thumbnail size and the two badge toggles are gone: one good thumbnail size, and the favorite and
// media-type badges always drawn.  the web app dropped both some time ago, and five copies of a
// density setting and four of each badge switch were the bulk of the settings screen.
//
// each table is rebuilt rather than altered.  `ALTER TABLE ... DROP COLUMN` needs sqlite 3.35, and
// the version that ships with the device is whatever that android release happened to bundle - so
// this does what room's own generated migrations do instead, which works on every one of them.
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        rebuild(
            db,
            table = "category_preference",
            createSql = """
                CREATE TABLE IF NOT EXISTS `category_preference_new` (
                    `id` INTEGER NOT NULL,
                    `display_type` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent(),
            columns = "`id`, `display_type`",
        )

        rebuild(
            db,
            table = "media_preference",
            createSql = """
                CREATE TABLE IF NOT EXISTS `media_preference_new` (
                    `id` INTEGER NOT NULL,
                    `slideshow_interval_seconds` INTEGER NOT NULL,
                    `show_face_highlights` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent(),
            columns = "`id`, `slideshow_interval_seconds`, `show_face_highlights`",
        )

        rebuild(
            db,
            table = "random_preference",
            createSql = """
                CREATE TABLE IF NOT EXISTS `random_preference_new` (
                    `id` INTEGER NOT NULL,
                    `slideshow_interval_seconds` INTEGER NOT NULL,
                    `show_widget_info` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent(),
            columns = "`id`, `slideshow_interval_seconds`, `show_widget_info`",
        )

        rebuild(
            db,
            table = "search_preference",
            createSql = """
                CREATE TABLE IF NOT EXISTS `search_preference_new` (
                    `id` INTEGER NOT NULL,
                    `recent_query_count` INTEGER NOT NULL,
                    `display_type` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent(),
            columns = "`id`, `recent_query_count`, `display_type`",
        )

        rebuild(
            db,
            table = "people_preference",
            createSql = """
                CREATE TABLE IF NOT EXISTS `people_preference_new` (
                    `id` INTEGER NOT NULL,
                    `sort_by` TEXT NOT NULL DEFAULT 'Name',
                    `show_names` INTEGER NOT NULL DEFAULT 1,
                    `show_media_counts` INTEGER NOT NULL DEFAULT 1,
                    `show_clans` INTEGER NOT NULL DEFAULT 1,
                    `show_category_year` INTEGER NOT NULL DEFAULT 1,
                    `show_category_title` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent(),
            columns = "`id`, `sort_by`, `show_names`, `show_media_counts`, `show_clans`, " +
                "`show_category_year`, `show_category_title`",
        )
    }

    // the columns that survive are copied across by name, so whatever else the old table held is
    // simply left behind with it
    private fun rebuild(
        db: SupportSQLiteDatabase,
        table: String,
        createSql: String,
        columns: String,
    ) {
        db.execSQL(createSql)
        db.execSQL("INSERT INTO `${table}_new` ($columns) SELECT $columns FROM `$table`")
        db.execSQL("DROP TABLE `$table`")
        db.execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
    }
}
