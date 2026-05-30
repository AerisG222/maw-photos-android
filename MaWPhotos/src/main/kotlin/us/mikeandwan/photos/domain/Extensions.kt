package us.mikeandwan.photos.domain

import java.net.HttpURLConnection
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.Category as ApiCategory
import us.mikeandwan.photos.api.Comment as ApiComment
import us.mikeandwan.photos.api.Media as ApiMedia
import us.mikeandwan.photos.api.MediaFile as ApiMediaFile
import us.mikeandwan.photos.database.CategoryDetail as DbCategoryDetail
import us.mikeandwan.photos.database.CategoryPreference as DbCategoryPreference
import us.mikeandwan.photos.database.MediaFileAndScale as DbMediaFileAndScale
import us.mikeandwan.photos.database.MediaPreference as DbMediaPreference
import us.mikeandwan.photos.database.NotificationPreference as DbNotificationPreference
import us.mikeandwan.photos.database.RandomPreference as DbRandomPreference
import us.mikeandwan.photos.database.Scale as DbScale
import us.mikeandwan.photos.database.SearchHistory as DbSearchHistory
import us.mikeandwan.photos.database.SearchPreference as DbSearchPreference
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

fun DbCategoryDetail.toDomainCategory(): Category =
    Category(
        id = category.id,
        year = category.year,
        name = category.name,
        effectiveDate = category.effectiveDate,
        modified = category.modified,
        isFavorite = category.isFavorite,
        teaser = mediaFiles.map { it.toDomainMediaFile() },
        mediaTypes = category.mediaTypes.map { getMediaType(it) },
    )

fun DbMediaFileAndScale.toDomainMediaFile(): MediaFile =
    MediaFile(
        scale = scale.toDomainScale(),
        type = getMediaFileType(mediaFile.type),
        path = mediaFile.path,
    )

private fun getMediaFileType(type: String): MediaFileType =
    when (type) {
        "photo" -> MediaFileType.Photo
        "video" -> MediaFileType.Video
        "video-poster" -> MediaFileType.VideoPoster
        else -> throw IllegalArgumentException("Unknown media file type: $type")
    }

fun DbScale.toDomainScale(): Scale =
    Scale(
        code = code,
        width = width,
        height = height,
        fillsDimensions = fillsDimensions,
    )

fun DbCategoryPreference.toDomainCategoryPreference(): CategoryPreference =
    CategoryPreference(
        displayType = displayType,
        gridThumbnailSize = gridThumbnailSize,
        showMediaTypeIndicator = showMediaTypeIndicator,
    )

fun DbNotificationPreference.toDomainNotificationPreference(): NotificationPreference =
    NotificationPreference(
        doNotify = doNotify,
        doVibrate = doVibrate,
    )

fun DbMediaPreference.toDomainPhotoPreference(): MediaPreference =
    MediaPreference(
        slideshowIntervalSeconds = slideshowIntervalSeconds,
        gridThumbnailSize = gridThumbnailSize,
        showMediaTypeIndicator = showMediaTypeIndicator,
    )

fun DbRandomPreference.toDomainRandomPreference(): RandomPreference =
    RandomPreference(
        slideshowIntervalSeconds = slideshowIntervalSeconds,
        gridThumbnailSize = gridThumbnailSize,
        showMediaTypeIndicator = showMediaTypeIndicator,
        showWidgetInfo = showWidgetInfo,
    )

fun ApiCategory.toDomainCategory(): Category =
    Category(
        id = id,
        year = effectiveDate.year,
        name = name,
        effectiveDate = effectiveDate,
        modified = modified,
        isFavorite = isFavorite,
        teaser = teaser.files.map { it.toDomainMediaFile() },
        mediaTypes = mediaTypes.map { getMediaType(it) },
    )

fun ApiMedia.toDomainMedia(): Media =
    Media(
        id = id,
        categoryId = categoryId,
        type = getMediaType(type),
        isFavorite = isFavorite,
        files = files.map { it.toDomainMediaFile() },
    )

// todo: we fake this here as we really only use the code - we try to reduce payload size by excluding scale info,
// but very inconvenient to have to query our db for this and trying not to settle for a static lookup...
fun ApiMediaFile.toDomainMediaFile(): MediaFile =
    MediaFile(
        scale = Scale(scale, 0, 0, false),
        type = getMediaFileType(type),
        path = path,
    )

private fun getMediaType(type: String): MediaType =
    when (type) {
        "photo" -> MediaType.Photo
        "video" -> MediaType.Video
        else -> throw IllegalArgumentException("Unknown media type: $type")
    }

fun ApiComment.toDomainComment(): Comment =
    Comment(
        commentId = commentId,
        created = created,
        createdBy = createdBy,
        modified = modified,
        body = body,
    )

fun DbSearchHistory.toDomainSearchHistory(): SearchHistory =
    SearchHistory(
        term = term,
        searchDate = searchDate,
    )

fun DbSearchPreference.toDomainSearchPreference(): SearchPreference =
    SearchPreference(
        id = id,
        recentQueryCountToSave = recentQueryCount,
        displayType = displayType,
        gridThumbnailSize = gridThumbnailSize,
        showMediaTypeIndicator = showMediaTypeIndicator,
    )

fun ApiResult.Error.isUnauthorized(): Boolean =
    errorCode == HttpURLConnection.HTTP_UNAUTHORIZED

fun Category.findTeaserImage(largerSize: Boolean): MediaFile = findTeaserImage(teaser, largerSize)

fun Media.findTeaserImage(largerSize: Boolean): MediaFile = findTeaserImage(files, largerSize)

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
            scale = Scale(code, 0, 0, false),
            type = MediaFileType.Photo,
            path = "",
        )
}
