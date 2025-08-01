package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MediaCategoryDao {
    @Query("""
       SELECT MAX(year) AS year FROM photo_category
    """)
    abstract fun getMostRecentYear(): Flow<Int?>

    @Query("""
       SELECT DISTINCT year
         FROM photo_category
        ORDER BY year DESC
    """)
    abstract fun getYears(): Flow<List<Int>>

    @Query("""
        SELECT pc.*,
               'Photo' AS category_type
          FROM photo_category pc
         WHERE year = :year
         ORDER BY id DESC
    """
    )
    abstract fun getCategoriesForYear(year: Int): Flow<List<MediaCategory>>
}
