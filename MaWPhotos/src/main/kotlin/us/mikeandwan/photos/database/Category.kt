package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "category",
    indices = [
        Index(value = ["year"], unique = false),
        Index(value = ["effective_date"], unique = false),
        Index(value = ["modified"], unique = false),
    ]
)
data class Category(
    @PrimaryKey val id: Uuid,
    val year: Int,
    val name: String,
    @ColumnInfo(name = "effective_date") val effectiveDate: Instant,
    @ColumnInfo(name = "modified") val modified: Instant,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean
)
