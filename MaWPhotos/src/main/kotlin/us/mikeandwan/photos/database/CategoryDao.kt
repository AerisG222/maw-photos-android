package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {
    @Query("""
        SELECT * 
        FROM category c 
        WHERE c.year = :year
        ORDER BY 
            c.effective_date DESC
    """
    )
    abstract fun getCategoriesForYear(year: Int): Flow<List<CategoryDetail>>

    @Query("""
        SELECT * 
        FROM category c  
        WHERE 
            id = :id
    """)
    abstract fun getCategory(id: Int): Flow<CategoryDetail?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(vararg categories: Category)
}
