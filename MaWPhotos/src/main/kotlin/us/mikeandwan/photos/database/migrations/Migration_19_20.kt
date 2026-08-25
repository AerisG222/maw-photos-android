package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// the clan row on the people screen can be folded away, and the fold is remembered.
//
// this is its own migration rather than part of the one that creates people_preference because
// version 19 had already been installed by the time the column was wanted: amending that migration
// left an installed database whose schema no longer matched its version, which Room refuses to open.
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE people_preference
                ADD COLUMN show_clans INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )
    }
}
