package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PeoplePreferenceDao {
    @Query("SELECT * FROM people_preference WHERE id = :id")
    fun getPeoplePreference(id: Int): Flow<PeoplePreference>

    @Upsert
    suspend fun setPeoplePreference(preference: PeoplePreference)
}
