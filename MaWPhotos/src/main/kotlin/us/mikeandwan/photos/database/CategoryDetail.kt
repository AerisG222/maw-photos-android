package us.mikeandwan.photos.database

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryDetail(
    @Embedded
    val category: Category,

    @Relation(
        entity = MediaFile::class,
        parentColumn = "id",
        entityColumn = "category_id"
    )
    val mediaFiles: List<MediaFile>
)
