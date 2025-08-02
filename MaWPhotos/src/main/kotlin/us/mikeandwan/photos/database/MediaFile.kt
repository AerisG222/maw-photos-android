package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "media_file",
    primaryKeys = [
        "category_id",
        "scale_id",
        "type"
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Scale::class,
            parentColumns = ["id"],
            childColumns = ["scale_id"]
        )
    ]
)
data class MediaFile(
    @ColumnInfo(name = "category_id") val categoryId: Uuid,
    @ColumnInfo(name = "scale_id") val scaleId: Uuid,
    val type: String,
    val path: String
)
