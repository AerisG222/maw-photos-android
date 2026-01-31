package us.mikeandwan.photos.api

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Scale(
    val id: Uuid,
    val code: String,
    val width: Int,
    val height: Int,
    val fillsDimensions: Boolean,
)
