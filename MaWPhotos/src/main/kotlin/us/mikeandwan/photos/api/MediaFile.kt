package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class MediaFile(
    val id: Uuid,
    val scale: String,
    val type: String,
    val path: String
)
