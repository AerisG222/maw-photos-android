package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.PersonSort

@Entity(tableName = "people_preference")
data class PeoplePreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "sort_by", defaultValue = "Name") val sortBy: PersonSort,
    @ColumnInfo(name = "grid_thumbnail_size") val gridThumbnailSize: GridThumbnailSize,
    @ColumnInfo(name = "show_names", defaultValue = "1") val showNames: Boolean,
    @ColumnInfo(name = "show_media_counts", defaultValue = "1") val showMediaCounts: Boolean,
)
