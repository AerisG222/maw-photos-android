package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.GridThumbnailSize

@Entity(tableName = "search_preference")
data class SearchPreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "recent_query_count") val recentQueryCount: Int,
    @ColumnInfo(name = "display_type") val displayType: CategoryDisplayType,
    @ColumnInfo(name = "grid_thumbnail_size") val gridThumbnailSize: GridThumbnailSize,
)
