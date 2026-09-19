package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "random_preference")
data class RandomPreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "slideshow_interval_seconds") val slideshowIntervalSeconds: Int,
    @ColumnInfo(name = "show_widget_info", defaultValue = "1") val showWidgetInfo: Boolean,
)
