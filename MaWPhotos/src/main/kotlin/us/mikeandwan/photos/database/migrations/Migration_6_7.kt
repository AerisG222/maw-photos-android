package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object: Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS year 
            (
                year INTEGER NOT NULL,
                PRIMARY KEY(year)
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scale 
            (
                id TEXT NOT NULL,
                code TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                fills_dimensions INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """)

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_scale_code 
                ON scale (code)
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS category 
            (
                id TEXT NOT NULL,
                year INTEGER NOT NULL,
                name TEXT NOT NULL,
                effective_date INTEGER NOT NULL,
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
