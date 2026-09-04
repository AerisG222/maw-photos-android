package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// the categories a person, a clan or a place turns up in can be labelled with their year, their
// title, both or neither, and the choice is remembered.
//
// two homes rather than one shared setting: the two areas share a feed but are read differently,
// and the rest of this screen already keeps a display preference per area.
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE people_preference
                ADD COLUMN show_category_year INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )

        db.execSQL(
            """
            ALTER TABLE people_preference
                ADD COLUMN show_category_title INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS place_preference (
                id INTEGER NOT NULL,
                show_category_year INTEGER NOT NULL DEFAULT 1,
                show_category_title INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )

        // the row the repository reads.  seeded here as well as in the create callback, which only
        // runs for a database that did not exist before this migration.
        db.execSQL(
            """
            INSERT OR IGNORE INTO place_preference (id, show_category_year, show_category_title)
                VALUES (1, 1, 1)
            """.trimIndent(),
        )
    }
}
