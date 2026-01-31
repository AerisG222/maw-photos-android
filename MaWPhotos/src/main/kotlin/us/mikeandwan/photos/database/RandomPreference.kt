package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.mikeandwan.photos.domain.models.GridThumbnailSize

@Entity(tableName = "random_preference")
data class RandomPreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "slideshow_interval_seconds") val slideshowIntervalSeconds: Int,
    @ColumnInfo(name = "grid_thumbnail_size") val gridThumbnailSize: GridThumbnailSize,
)
