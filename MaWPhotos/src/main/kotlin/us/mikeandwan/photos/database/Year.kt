package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "year")
data class Year(
    @PrimaryKey val year: Int,
    @ColumnInfo(name = "has_initialized_categories") val hasInitializedCategories: Boolean = false
)
