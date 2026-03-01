package us.mikeandwan.photos.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPreferenceDao {
    @Query("SELECT * FROM notification_preference WHERE id = :id")
    fun getNotificationPreference(id: Int): Flow<NotificationPreference>

    @Upsert
    suspend fun setNotificationPreference(preference: NotificationPreference)
}
