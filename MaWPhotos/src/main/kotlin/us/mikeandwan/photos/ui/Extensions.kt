package us.mikeandwan.photos.ui

import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.SearchResultCategory
import us.mikeandwan.photos.ui.controls.imagegrid.ImageGridItem
import kotlin.time.Clock

fun Media.toImageGridItem(): ImageGridItem<Media> {
    return ImageGridItem (
        this.id,
        "TODO",
        this
    )
}

fun Media.getMediaUrl(): String {
    return "TODO"
}

fun Category.toImageGridItem(): ImageGridItem<Category> {
    return ImageGridItem(
        this.id,
        this.teaser.first().path,
        this
    )
}

fun SearchResultCategory.toDomainMediaCategory(): Category {
    return Category(
        this.id,
        this.year,
        this.name,
        Clock.System.now(),
        Clock.System.now(),
        false,
        emptyList()
    )
}

fun <T> ApiResult<T>.toExternalCallStatus(): ExternalCallStatus<T> {
    return when(this) {
        is ApiResult.Success -> ExternalCallStatus.Success(this.result)
        is ApiResult.Empty -> ExternalCallStatus.Error("Unexpected result")
        is ApiResult.Error -> ExternalCallStatus.Error(this.error, this.exception)
    }
}
