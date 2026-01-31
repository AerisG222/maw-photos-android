package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(
    val id: Uuid,
    val scale: String,
    val type: String,
    val path: String,
)
