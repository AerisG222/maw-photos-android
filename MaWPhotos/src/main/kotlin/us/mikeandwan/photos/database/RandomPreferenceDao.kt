package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RandomPreferenceDao {
    @Query("SELECT * FROM random_preference WHERE id = :id")
    fun getRandomPreference(id: Int): Flow<RandomPreference>

    @Upsert
    suspend fun setRandomPreference(preference: RandomPreference)
}
