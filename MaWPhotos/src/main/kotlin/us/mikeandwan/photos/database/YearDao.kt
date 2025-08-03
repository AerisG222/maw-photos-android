package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YearDao {
    @Query("SELECT year FROM year ORDER BY year DESC")
    fun getYears(): Flow<List<Int>>

    @Query("SELECT MAX(year) FROM year")
    fun getMostRecentYear(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg years: Year)
}
