package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
abstract class CategoryDao {
    @Transaction
    @Query("""
        SELECT * 
        FROM category c 
        WHERE c.year = :year
        ORDER BY 
            c.effective_date DESC
    """
    )
    abstract fun getCategoriesForYear(year: Int): Flow<List<CategoryDetail>>

    @Transaction
    @Query("""
        SELECT * 
        FROM category c  
        WHERE 
            id = :id
    """)
    abstract fun getCategory(id: Int): Flow<CategoryDetail?>

    @Query("""
        SELECT MAX(modified)
        FROM category
    """
    )
    abstract fun getMostRecentModifiedDate(): Flow<Instant?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(vararg categories: Category)
}
