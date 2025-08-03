package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

abstract class Media {
    abstract var type: MediaType
    abstract var id: Uuid
    abstract var categoryId: Uuid
    abstract var thumbnailHeight: Int
    abstract var thumbnailWidth: Int
    abstract var thumbnailUrl: String
}
