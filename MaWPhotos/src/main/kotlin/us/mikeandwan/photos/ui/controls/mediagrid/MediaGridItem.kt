package us.mikeandwan.photos.ui.controls.mediagrid

import kotlin.uuid.Uuid

data class MediaGridItem<T> (
    val id: Uuid,
    val url: String,
    val data: T
)
