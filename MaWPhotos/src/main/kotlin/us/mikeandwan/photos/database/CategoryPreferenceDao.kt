package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryPreferenceDao {
    @Query("SELECT * FROM category_preference WHERE id = :id")
    fun getCategoryPreference(id: Int): Flow<CategoryPreference>

    @Upsert
    suspend fun setCategoryPreference(preference: CategoryPreference)
}
