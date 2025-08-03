package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class SearchResultCategory(
    val solrId: String,
    val id: Uuid,
    val year: Int,
    val name: String,
    val multimediaType: String,
    val teaserPhotoHeight: Int,
    val teaserPhotoWidth: Int,
    val teaserPhotoPath: String,
    val teaserPhotoSqHeight: Int,
    val teaserPhotoSqWidth: Int,
    val teaserPhotoSqPath: String,
    val score: Double
)
