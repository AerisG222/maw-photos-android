package us.mikeandwan.photos.datastore

import android.database.Cursor
import androidx.datastore.core.DataMigration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber
import us.mikeandwan.photos.database.MawDatabase

/**
 * Carries the settings across from the tables they were kept in before the preferences moved to
 * DataStore, then drops those tables.
 *
 * The room migration that stopped declaring them (22 -> 23) deliberately left them in place, so
 * this can run whenever the store is first read, whichever of the two happens to open first.  With
 * the tables gone there is nothing left to import, which is also how a fresh install - one that
 * never had them - is told apart.
 *
 * Each table is read on its own and a failure only costs that table's settings: a preferences
 * store that cannot finish its migration cannot be read at all, which would be far worse than a
 * setting quietly going back to its default.
 */
class RoomPreferencesImport(
    private val database: MawDatabase,
) : DataMigration<UserPreferences> {
    companion object {
        private const val PREFERENCE_ID = 1

        val LEGACY_TABLES = listOf(
            "category_preference",
            "media_preference",
            "notification_preference",
            "people_preference",
            "place_preference",
            "random_preference",
            "search_preference",
        )
    }

    private val db: SupportSQLiteDatabase
        get() = database.openHelper.writableDatabase

    override suspend fun shouldMigrate(currentData: UserPreferences) = LEGACY_TABLES.any { tableExists(it) }

    override suspend fun migrate(currentData: UserPreferences): UserPreferences {
        var prefs = currentData

        readRow("category_preference") { c ->
            prefs = prefs.copy(
                category = prefs.category.copy(
                    displayType = c.getEnum("display_type", prefs.category.displayType),
                ),
            )
        }

        readRow("media_preference") { c ->
            prefs = prefs.copy(
                media = prefs.media.copy(
                    slideshowIntervalSeconds = c.getInt("slideshow_interval_seconds"),
                    showFaceHighlights = c.getBoolean("show_face_highlights"),
                ),
            )
        }

        readRow("notification_preference") { c ->
            prefs = prefs.copy(
                notification = prefs.notification.copy(
                    doNotify = c.getBoolean("do_notify"),
                    doVibrate = c.getBoolean("do_vibrate"),
                ),
            )
        }

        readRow("people_preference") { c ->
            prefs = prefs.copy(
                people = prefs.people.copy(
                    sortBy = c.getEnum("sort_by", prefs.people.sortBy),
                    showNames = c.getBoolean("show_names"),
                    showMediaCounts = c.getBoolean("show_media_counts"),
                    showClans = c.getBoolean("show_clans"),
                    showCategoryYear = c.getBoolean("show_category_year"),
                    showCategoryTitle = c.getBoolean("show_category_title"),
                ),
            )
        }

        readRow("place_preference") { c ->
            prefs = prefs.copy(
                place = prefs.place.copy(
                    showCategoryYear = c.getBoolean("show_category_year"),
                    showCategoryTitle = c.getBoolean("show_category_title"),
                ),
            )
        }

        readRow("random_preference") { c ->
            prefs = prefs.copy(
                random = prefs.random.copy(
                    slideshowIntervalSeconds = c.getInt("slideshow_interval_seconds"),
                    showWidgetInfo = c.getBoolean("show_widget_info"),
                ),
            )
        }

        readRow("search_preference") { c ->
            prefs = prefs.copy(
                search = prefs.search.copy(
                    recentQueryCountToSave = c.getInt("recent_query_count"),
                    displayType = c.getEnum("display_type", prefs.search.displayType),
                ),
            )
        }

        return prefs
    }

    // only called once the imported settings have been written, so dropping the tables here can
    // never lose anything that has not already been carried across
    override suspend fun cleanUp() {
        LEGACY_TABLES.forEach { db.execSQL("DROP TABLE IF EXISTS `$it`") }
    }

    private fun tableExists(table: String) =
        db
            .query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
            .use { it.moveToFirst() }

    private fun readRow(
        table: String,
        read: (Cursor) -> Unit,
    ) {
        try {
            if (tableExists(table)) {
                db.query("SELECT * FROM `$table` WHERE id = ?", arrayOf(PREFERENCE_ID)).use { c ->
                    if (c.moveToFirst()) {
                        read(c)
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Unable to import the settings in $table - they will be reset to their defaults")
        }
    }

    private fun Cursor.getInt(column: String) = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.getBoolean(column: String) = getInt(column) != 0

    // room stored enums by name
    private inline fun <reified T : Enum<T>> Cursor.getEnum(
        column: String,
        default: T,
    ): T {
        val name = getString(getColumnIndexOrThrow(column))

        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
