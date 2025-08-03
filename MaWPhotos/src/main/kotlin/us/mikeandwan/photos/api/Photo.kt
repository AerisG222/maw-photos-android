package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Photo(
    val id: Uuid,
    val categoryId: Uuid,
    val imageXs: MultimediaAsset,
    val imageSm: MultimediaAsset,
    val imageMd: MultimediaAsset,
    val imageLg: MultimediaAsset
)
