package us.mikeandwan.photos.ui.components.mediagrid

import us.mikeandwan.photos.domain.models.MediaType
import kotlin.uuid.Uuid

data class MediaGridItem<T>(
    val id: Uuid,
    val url: String,
    val mediaTypes: List<MediaType>,
    val data: T,
)
