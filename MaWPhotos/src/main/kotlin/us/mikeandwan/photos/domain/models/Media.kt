package us.mikeandwan.photos.domain.models

import us.mikeandwan.photos.api.MediaFile
import kotlin.uuid.Uuid

// todo: add categoryId?
data class Media (
    val id: Uuid,
    val categoryId: Uuid,
    val type: MediaType,
    val isFavorite: Boolean,
    val files: List<MediaFile> = ArrayList()
)
