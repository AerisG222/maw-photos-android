package us.mikeandwan.photos.ui

import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.findTeaserImage
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.controls.mediagrid.MediaGridItem

fun Media.toMediaGridItem(useLargeTeaser: Boolean): MediaGridItem<Media> {
    return MediaGridItem (
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        this.type == MediaType.Video,
        this
    )
}

val preferredMediaScales = arrayOf("qhd", "full-hd", "nhd", "qvg")

fun Media.getMediaUrl(): String {
    for(scale in preferredMediaScales) {
        this.files.find{ it.scale.code == scale }?.let {
            return it.path
        }
    }

    return ""
}

fun Category.toMediaGridItem(useLargeTeaser: Boolean): MediaGridItem<Category> {
    return MediaGridItem(
        this.id,
        this.findTeaserImage(useLargeTeaser).path,
        false,
        this
    )
}

fun <T> ApiResult<T>.toExternalCallStatus(): ExternalCallStatus<T> {
    return when(this) {
        is ApiResult.Success -> ExternalCallStatus.Success(this.result)
        is ApiResult.Empty -> ExternalCallStatus.Error("Unexpected result")
        is ApiResult.Error -> ExternalCallStatus.Error(this.error, this.exception)
    }
}
