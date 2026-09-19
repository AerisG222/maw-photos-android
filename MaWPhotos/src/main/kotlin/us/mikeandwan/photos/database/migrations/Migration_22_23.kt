package us.mikeandwan.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// the preference tables are no longer room's - the settings now live in datastore.  they are left
// exactly as they are rather than dropped here: RoomPreferencesImport reads them the first time the
// settings are, and drops them itself once what they held has been written out.  dropping them here
// instead would throw the settings away whenever the database happened to be opened first.
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}
