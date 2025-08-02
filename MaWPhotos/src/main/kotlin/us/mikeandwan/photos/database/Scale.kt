package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "scale",
    indices = [Index(value = ["code"], unique = true)]
)
data class Scale(
    @PrimaryKey val id: Uuid,
    val code: String,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "fills_dimensions") val fillsDimensions: Boolean
)
