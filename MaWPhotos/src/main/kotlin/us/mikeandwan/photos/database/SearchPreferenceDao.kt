package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchPreferenceDao {
    @Query("SELECT * FROM search_preference WHERE id = :id")
    fun getSearchPreference(id: Int): Flow<SearchPreference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSearchPreference(preference: SearchPreference)
}
