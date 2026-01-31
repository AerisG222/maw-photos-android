package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey val term: String,
    @ColumnInfo(name = "search_date") val searchDate: Calendar,
)
