package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS developer_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                level TEXT NOT NULL,
                throwable TEXT
            )
            """.trimIndent(),
        )
    }
}
