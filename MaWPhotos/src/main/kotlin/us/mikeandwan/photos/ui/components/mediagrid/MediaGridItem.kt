package us.mikeandwan.photos.ui.components.mediagrid

import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.MediaType

data class MediaGridItem<T>(
    val id: Uuid,
    val url: String,
    val mediaTypes: List<MediaType>,
    val data: T,
)
