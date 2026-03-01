package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaPreferenceDao {
    @Query("SELECT * FROM media_preference WHERE id = :id")
    fun getPhotoPreference(id: Int): Flow<MediaPreference>

    @Upsert
    suspend fun setPhotoPreference(preference: MediaPreference)
}
