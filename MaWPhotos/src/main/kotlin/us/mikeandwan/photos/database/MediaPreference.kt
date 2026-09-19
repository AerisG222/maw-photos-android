package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_preference")
data class MediaPreference(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "slideshow_interval_seconds") val slideshowIntervalSeconds: Int,
    // one setting rather than one per view: somebody who wants faces marked generally wants them
    // marked wherever media is shown.  off by default, because nothing about faces is fetched until
    // it is on.
    @ColumnInfo(name = "show_face_highlights", defaultValue = "0") val showFaceHighlights: Boolean,
)
