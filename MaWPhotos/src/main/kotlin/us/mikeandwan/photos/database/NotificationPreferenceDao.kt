package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPreferenceDao {
    @Query("SELECT * FROM notification_preference WHERE id = :id")
    fun getNotificationPreference(id: Int): Flow<NotificationPreference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setNotificationPreference(preference: NotificationPreference)
}
