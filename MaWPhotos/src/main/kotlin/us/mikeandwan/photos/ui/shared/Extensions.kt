package us.mikeandwan.photos.ui.shared

import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.findTeaserImage
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem

fun Media.toMediaGridItem(useLargeTeaser: Boolean): MediaGridItem<Media> =
    MediaGridItem(
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        this.type == MediaType.Video,
        this,
    )

val preferredMediaScales = arrayOf("qhd", "full-hd", "nhd", "qvg")

fun Media.getMediaUrl(): String {
    for (scale in preferredMediaScales) {
        this.files.find { it.scale.code == scale }?.let {
            return it.path
        }
    }

    return ""
}

fun Category.toMediaGridItem(useLargeTeaser: Boolean): MediaGridItem<Category> =
    MediaGridItem(
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        false,
        this,
    )

fun <T> ApiResult<T>.toExternalCallStatus(): ExternalCallStatus<T> =
    when (this) {
        is ApiResult.Success -> ExternalCallStatus.Success(this.result)
        is ApiResult.Empty -> ExternalCallStatus.Error("Unexpected result")
        is ApiResult.Error -> ExternalCallStatus.Error(this.error, this.exception)
    }
