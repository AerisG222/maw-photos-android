package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val id: Uuid,
    val categoryId: Uuid,
    val type: String,
    val isFavorite: Boolean,
    val files: List<MediaFile> = ArrayList(),
)
