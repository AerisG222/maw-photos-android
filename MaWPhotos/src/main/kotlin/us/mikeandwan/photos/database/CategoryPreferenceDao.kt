package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryPreferenceDao {
    @Query("SELECT * FROM category_preference WHERE id = :id")
    fun getCategoryPreference(id: Int): Flow<CategoryPreference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCategoryPreference(preference: CategoryPreference)
}
