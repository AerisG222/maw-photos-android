package us.mikeandwan.photos.database

import androidx.room.Embedded
import androidx.room.Relation

data class MediaFileAndScale(
    @Embedded
    val mediaFile: MediaFile,

    @Relation(
        parentColumn = "scale_id",
        entityColumn = "id"
    )
    val scale: Scale
)
