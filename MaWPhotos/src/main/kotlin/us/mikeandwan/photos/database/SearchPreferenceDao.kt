package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchPreferenceDao {
    @Query("SELECT * FROM search_preference WHERE id = :id")
    fun getSearchPreference(id: Int): Flow<SearchPreference>

    @Upsert
    suspend fun setSearchPreference(preference: SearchPreference)
}
