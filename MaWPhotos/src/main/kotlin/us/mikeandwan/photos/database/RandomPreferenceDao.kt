package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RandomPreferenceDao {
    @Query("SELECT * FROM random_preference WHERE id = :id")
    fun getRandomPreference(id: Int): Flow<RandomPreference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRandomPreference(preference: RandomPreference)
}
