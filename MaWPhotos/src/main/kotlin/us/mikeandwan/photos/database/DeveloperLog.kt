package us.mikeandwan.photos.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "developer_log")
data class DeveloperLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val timestamp: Instant,
    val level: String, // "INFO" or "ERROR"
    val throwable: String? = null,
)
