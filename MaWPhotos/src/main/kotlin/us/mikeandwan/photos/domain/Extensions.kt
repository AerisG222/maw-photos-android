package us.mikeandwan.photos.domain

import java.net.HttpURLConnection
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFile
import us.mikeandwan.photos.domain.models.MediaFileType
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.domain.models.NotificationPreference
import us.mikeandwan.photos.domain.models.RandomPreference
import us.mikeandwan.photos.domain.models.Scale
import us.mikeandwan.photos.domain.models.SearchHistory
import us.mikeandwan.photos.domain.models.SearchPreference

fun us.mikeandwan.photos.database.CategoryDetail.toDomainCategory(): Category =
    Category(
        this.category.id,
        this.category.year,
        this.category.name,
        this.category.effectiveDate,
        this.category.modified,
        this.category.isFavorite,
        this.mediaFiles.map { it.toDomainMediaFile() },
        this.category.mediaTypes.map { getMediaType(it) },
    )

fun us.mikeandwan.photos.database.MediaFileAndScale.toDomainMediaFile(): MediaFile =
    MediaFile(
        this.scale.toDomainScale(),
        getMediaFileType(this.mediaFile.type),
        this.mediaFile.path,
    )

fun getMediaFileType(type: String): MediaFileType =
    when (type) {
        "photo" -> MediaFileType.Photo
        "video" -> MediaFileType.Video
        "video-poster" -> MediaFileType.VideoPoster
        else -> throw IllegalArgumentException("Unknown media file type: $type")
    }

fun us.mikeandwan.photos.database.Scale.toDomainScale(): Scale =
    Scale(
        this.code,
        this.width,
        this.height,
        this.fillsDimensions,
    )

fun us.mikeandwan.photos.database.CategoryPreference.toDomainCategoryPreference(): CategoryPreference =
    CategoryPreference(
        this.displayType,
        this.gridThumbnailSize,
    )

fun us.mikeandwan.photos.database.NotificationPreference.toDomainNotificationPreference(): NotificationPreference =
    NotificationPreference(
        this.doNotify,
        this.doVibrate,
    )

fun us.mikeandwan.photos.database.MediaPreference.toDomainPhotoPreference(): MediaPreference =
    MediaPreference(
        this.slideshowIntervalSeconds,
        this.gridThumbnailSize,
    )

fun us.mikeandwan.photos.database.RandomPreference.toDomainRandomPreference(): RandomPreference =
    RandomPreference(
        this.slideshowIntervalSeconds,
        this.gridThumbnailSize,
    )

fun us.mikeandwan.photos.api.Category.toDomainCategory(): Category =
    Category(
        this.id,
        this.effectiveDate.year,
        this.name,
        this.effectiveDate,
        this.modified,
        this.isFavorite,
        this.teaser.files.map { it.toDomainMediaFile() },
        this.mediaTypes.map { getMediaType(it) },
    )

fun us.mikeandwan.photos.api.Media.toDomainMedia(): Media =
    Media(
        this.id,
        this.categoryId,
        getMediaType(this.type),
        this.isFavorite,
        this.files.map { it.toDomainMediaFile() },
    )

// todo: we fake this here as we really only use the code - we try to reduce payload size by excluding scale info,
// but very inconvenient to have to query our db for this and trying not to settle for a static lookup...
fun us.mikeandwan.photos.api.MediaFile.toDomainMediaFile(): MediaFile =
    MediaFile(
        Scale(this.scale, 0, 0, false),
        getMediaFileType(this.type),
        this.path,
    )

fun getMediaType(type: String): MediaType =
    when (type) {
        "photo" -> MediaType.Photo
        "video" -> MediaType.Video
        else -> throw IllegalArgumentException("Unknown media type: $type")
    }

fun us.mikeandwan.photos.api.Comment.toDomainComment(): Comment =
    Comment(
        this.commentId,
        this.created,
        this.createdBy,
        this.modified,
        this.body,
    )

fun us.mikeandwan.photos.database.SearchHistory.toDomainSearchHistory(): SearchHistory =
    SearchHistory(
        this.term,
        this.searchDate,
    )

fun us.mikeandwan.photos.database.SearchPreference.toDomainSearchPreference(): SearchPreference =
    SearchPreference(
        this.id,
        this.recentQueryCount,
        this.displayType,
        this.gridThumbnailSize,
    )

fun us.mikeandwan.photos.api.ApiResult.Error.isUnauthorized(): Boolean =
    this.errorCode == HttpURLConnection.HTTP_UNAUTHORIZED

fun Category.findTeaserImage(largerSize: Boolean): MediaFile = findTeaserImage(this.teaser, largerSize)

fun Media.findTeaserImage(largerSize: Boolean): MediaFile = findTeaserImage(this.files, largerSize)

fun findTeaserImage(
    files: List<MediaFile>,
    largerSize: Boolean,
): MediaFile {
    val code = if (largerSize) "qvg-fill" else "qqvg-fill"

    return files.find {
        (it.type == MediaFileType.Photo || it.type == MediaFileType.VideoPoster) &&
            it.scale.code == code
    }
        ?: MediaFile(
            Scale(code, 0, 0, false),
            MediaFileType.Photo,
            "",
        )
}
