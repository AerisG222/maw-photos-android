package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_preference")
data class NotificationPreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "do_notify") val doNotify: Boolean,
    @ColumnInfo(name = "do_vibrate") val doVibrate: Boolean,
)
