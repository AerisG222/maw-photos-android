package us.mikeandwan.photos.ui.controls.imagegrid

import kotlin.uuid.Uuid

data class ImageGridItem<T> (
    val id: Uuid,
    val url: String,
    val data: T
)
