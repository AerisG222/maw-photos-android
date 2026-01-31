package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS video_category (
                id INTEGER NOT NULL,
                year INTEGER NOT NULL,
                name TEXT NOT NULL,
                teaser_height INTEGER NOT NULL,
                teaser_width INTEGER NOT NULL,
                teaser_url TEXT NOT NULL,
                PRIMARY KEY(id)
            )
        """,
        )

        db.execSQL(
            """
           CREATE INDEX IF NOT EXISTS index_video_category_year
               ON video_category (year)
        """,
        )
    }
}
