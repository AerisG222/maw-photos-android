package us.mikeandwan.photos

import androidx.room.Room.databaseBuilder
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.migrations.ALL_MIGRATIONS
import us.mikeandwan.photos.datastore.RoomPreferencesImport
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.NotificationPreference
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PersonSort
import us.mikeandwan.photos.domain.models.PlacePreference
import us.mikeandwan.photos.domain.models.RandomPreference
import us.mikeandwan.photos.domain.models.SearchPreference

class RoomPreferencesImportTests {
    private val testDb = "preferences-import-test"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var database: MawDatabase? = null

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MawDatabase::class.java,
    )

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(testDb)
    }

    @Test
    fun importsEverySettingAndDropsTheTables() =
        runBlocking {
            // every value differs from its default, so a setting that was not carried across shows
            helper.createDatabase(testDb, 22).apply {
                execSQL("INSERT INTO category_preference (id, display_type) VALUES (1, 'List')")
                execSQL("INSERT INTO notification_preference (id, do_notify, do_vibrate) VALUES (1, 1, 0)")
                execSQL(
                    "INSERT INTO media_preference (id, slideshow_interval_seconds, show_face_highlights) VALUES (1, 7, 1)",
                )
                execSQL(
                    "INSERT INTO people_preference (id, sort_by, show_names, show_media_counts, show_clans, show_category_year, show_category_title) VALUES (1, 'MediaCount', 0, 0, 0, 0, 0)",
                )
                execSQL("INSERT INTO place_preference (id, show_category_year, show_category_title) VALUES (1, 0, 0)")
                execSQL(
                    "INSERT INTO random_preference (id, slideshow_interval_seconds, show_widget_info) VALUES (1, 9, 0)",
                )
                execSQL(
                    "INSERT INTO search_preference (id, recent_query_count, display_type) VALUES (1, 5, 'List')",
                )
                close()
            }

            val import = RoomPreferencesImport(openDatabase())

            assertTrue(import.shouldMigrate(UserPreferences()))

            val imported = import.migrate(UserPreferences())

            assertEquals(
                UserPreferences(
                    category = CategoryPreference(CategoryDisplayType.List),
                    media = MediaPreference(slideshowIntervalSeconds = 7, showFaceHighlights = true),
                    notification = NotificationPreference(doNotify = true, doVibrate = false),
                    people = PeoplePreference(
                        sortBy = PersonSort.MediaCount,
                        showNames = false,
                        showMediaCounts = false,
                        showClans = false,
                        showCategoryYear = false,
                        showCategoryTitle = false,
                    ),
                    place = PlacePreference(showCategoryYear = false, showCategoryTitle = false),
                    random = RandomPreference(slideshowIntervalSeconds = 9, showWidgetInfo = false),
                    search = SearchPreference(recentQueryCountToSave = 5, displayType = CategoryDisplayType.List),
                ),
                imported,
            )

            import.cleanUp()

            assertFalse(import.shouldMigrate(imported))
        }

    @Test
    fun freshInstallHasNothingToImport() =
        runBlocking {
            assertFalse(RoomPreferencesImport(openDatabase()).shouldMigrate(UserPreferences()))
        }

    private fun openDatabase() =
        databaseBuilder(context, MawDatabase::class.java, testDb)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
            .also { database = it }
}
