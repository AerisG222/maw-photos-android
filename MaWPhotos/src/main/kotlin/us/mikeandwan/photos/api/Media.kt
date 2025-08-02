package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Media(
    val id: Uuid,
    val type: String,
    val isFavorite: Boolean,
    val files: List<MediaFile> = ArrayList()
)
