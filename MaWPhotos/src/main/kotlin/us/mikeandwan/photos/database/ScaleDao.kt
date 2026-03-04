package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScaleDao {
    @Query("SELECT * FROM scale ORDER BY width")
    fun getScales(): Flow<List<Scale>>

    @Upsert
    suspend fun upsert(scale: List<Scale>)
}
