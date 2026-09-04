package us.mikeandwan.photos.ui.components.mediagrid

import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.MediaType

data class MediaGridItem<T>(
    val id: Uuid,
    val url: String,
    val mediaTypes: List<MediaType>,
    val data: T,
    val isFavorite: Boolean = false,
    // drawn along the bottom of the tile when there is one.  null for a photograph, which has no
    // name of its own - only a category does, and only when the screen listing it asks for one.
    val label: String? = null,
)
