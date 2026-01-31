package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Calendar
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY search_date DESC")
    fun getSearchTerms(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSearchTerm(term: SearchHistory)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()

    @Query("SELECT search_date FROM search_history ORDER BY search_date DESC LIMIT 1 OFFSET :queriesToKeep")
    suspend fun getEarliestDateToRemove(queriesToKeep: Int): Calendar

    @Query("DELETE FROM search_history WHERE search_date <= :earliestDate")
    suspend fun removeOldHistory(earliestDate: Calendar)
}
