import androidx.room.Room.databaseBuilder
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.migrations.MIGRATION_1_2
import us.mikeandwan.photos.database.migrations.MIGRATION_2_3
import us.mikeandwan.photos.database.migrations.MIGRATION_3_4

class RoomMigrationTests {
    private val testDb = "migration-test"

    private val allMigrations = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MawDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MawDatabase::class.java,
            testDb,
        ).addMigrations(*allMigrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
