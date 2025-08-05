package us.mikeandwan.photos.ui

import kotlinx.datetime.LocalDate
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.SearchResultCategory
import us.mikeandwan.photos.ui.controls.mediagrid.MediaGridItem
import kotlin.time.Clock

fun Media.toImageGridItem(): MediaGridItem<Media> {
    return MediaGridItem (
        this.id,
        "TODO",
        this
    )
}

fun Media.getMediaUrl(): String {
    return "TODO"
}

fun Category.toImageGridItem(): MediaGridItem<Category> {
    return MediaGridItem(
        this.id,
        "x", //this.teaser.first().path,
        this
    )
}

fun SearchResultCategory.toDomainMediaCategory(): Category {
    return Category(
        this.id,
        this.year,
        this.name,
        LocalDate.parse("2023-01-01"),
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
