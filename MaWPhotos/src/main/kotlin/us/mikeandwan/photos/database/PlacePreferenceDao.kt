package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacePreferenceDao {
    @Query("SELECT * FROM place_preference WHERE id = :id")
    fun getPlacePreference(id: Int): Flow<PlacePreference>

    @Upsert
    suspend fun setPlacePreference(preference: PlacePreference)
}
