package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Scale(
    val id: Uuid,
    val code: String,
    val width: Int,
    val height: Int,
    val fillsDimensions: Boolean
)
