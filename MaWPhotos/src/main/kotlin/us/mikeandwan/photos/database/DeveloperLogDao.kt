package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeveloperLogDao {
    @Insert
    suspend fun insert(log: DeveloperLog)

    @Query("SELECT * FROM developer_log ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<DeveloperLog>>

    @Query("DELETE FROM developer_log")
    suspend fun clearAll()

    @Query("DELETE FROM developer_log WHERE id NOT IN (SELECT id FROM developer_log ORDER BY timestamp DESC LIMIT 50)")
    suspend fun pruneOldLogs()
}
