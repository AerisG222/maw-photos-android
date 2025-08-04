package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object: Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE media_file")
        db.execSQL("DROP TABLE category")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS category 
            (
                id TEXT NOT NULL,
                year INTEGER NOT NULL,
                name TEXT NOT NULL,
                effective_date TEXT NOT NULL,
                modified INTEGER NOT NULL,
                is_favorite INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """)

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_category_year 
                ON category (year)
        """)

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_category_effective_date 
                ON category (effective_date)
        """)

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_category_modified 
                ON category (modified)
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS media_file 
            (
                category_id TEXT NOT NULL,
                scale_id TEXT NOT NULL,
                type TEXT NOT NULL,
                path TEXT NOT NULL,
                PRIMARY KEY(category_id, scale_id, type),
                FOREIGN KEY(category_id) REFERENCES category(id) ON DELETE CASCADE,
                FOREIGN KEY(scale_id) REFERENCES scale(id)
            )
        """)
    }
}
