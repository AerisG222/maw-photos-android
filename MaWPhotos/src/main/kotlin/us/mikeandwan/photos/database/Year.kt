package us.mikeandwan.photos.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "year")
data class Year(
    @PrimaryKey val year: Int
)
