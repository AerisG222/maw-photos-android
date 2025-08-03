package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScaleDao {
    @Query("SELECT * FROM scale ORDER BY width")
    fun getScales(): Flow<List<Scale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg scale: Scale)
}
