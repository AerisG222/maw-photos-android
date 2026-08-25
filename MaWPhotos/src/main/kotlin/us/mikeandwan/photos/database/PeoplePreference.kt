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
    // the clan row costs about a quarter of a phone screen before a single face is drawn, so it can
    // be folded away by whoever is here to look at people rather than at their groups
    @ColumnInfo(name = "show_clans", defaultValue = "1") val showClans: Boolean,
)
