package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable

@Serializable
data class UploadedFile(
    val name: String,
    val size: Long
)
