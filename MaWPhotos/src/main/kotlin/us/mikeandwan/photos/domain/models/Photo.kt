package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

data class Photo (
    override var type: MediaType,
    override var id: Uuid,
    override var categoryId: Uuid,
    override var thumbnailHeight: Int,
    override var thumbnailWidth: Int,
    override var thumbnailUrl: String,
    val mdHeight: Int,
    val mdWidth: Int,
    val mdUrl: String,
) : Media()
