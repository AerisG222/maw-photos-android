package us.mikeandwan.photos.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import us.mikeandwan.photos.Constants
import us.mikeandwan.photos.domain.models.*
import java.net.HttpURLConnection
import java.util.Calendar
import kotlin.time.*

fun us.mikeandwan.photos.database.PhotoCategory.toDomainMediaCategory(): MediaCategory {
    return MediaCategory(
        MediaType.Photo,
        this.id,
        this.year,
        this.name,
        this.teaserHeight,
        this.teaserWidth,
        this.teaserUrl
    )
}


fun us.mikeandwan.photos.database.MediaCategory.toDomainMediaCategory(): MediaCategory {
    return MediaCategory(
        this.type,
        this.id,
        this.year,
        this.name,
        this.teaserHeight,
        this.teaserWidth,
        this.teaserUrl
    )
}

fun us.mikeandwan.photos.api.Category.toDatabaseCategory(): us.mikeandwan.photos.database.Category {
    return us.mikeandwan.photos.database.Category(
        this.id,
        this.effectiveDate.toLocalDateTime(TimeZone.currentSystemDefault()).year,
        this.name,
        this.effectiveDate,
        this.modified,
        this.isFavorite
    )
}

fun us.mikeandwan.photos.database.CategoryPreference.toDomainCategoryPreference(): CategoryPreference {
    return CategoryPreference(
        this.displayType,
        this.gridThumbnailSize
    )
}

fun us.mikeandwan.photos.database.NotificationPreference.toDomainNotificationPreference(): NotificationPreference {
    return NotificationPreference(
        this.doNotify,
        this.doVibrate
    )
}


fun us.mikeandwan.photos.database.MediaPreference.toDomainPhotoPreference(): MediaPreference {
    return MediaPreference(
        this.slideshowIntervalSeconds,
        this.gridThumbnailSize
    )
}

fun us.mikeandwan.photos.database.RandomPreference.toDomainRandomPreference(): RandomPreference {
    return RandomPreference(
        this.slideshowIntervalSeconds,
        this.gridThumbnailSize
    )
}

fun us.mikeandwan.photos.api.Photo.toDomainPhoto(): Photo {
    return Photo(
        MediaType.Photo,
        this.id,
        this.categoryId,
        this.imageXs.height,
        this.imageXs.width,
        this.imageXs.url,
        this.imageMd.height,
        this.imageMd.width,
        this.imageMd.url
    )
}

fun us.mikeandwan.photos.api.Rating.toDomainRating(): Rating {
    return Rating(
        this.userRating,
        this.averageRating
    )
}

fun us.mikeandwan.photos.api.Comment.toDomainComment(): Comment {
    return Comment(
        this.entryDate,
        this.commentText,
        this.username
    )
}

fun us.mikeandwan.photos.api.ExifData.toDomainExifData(): ExifData {
    return ExifData(
        // exif
        this.bitsPerSample,
        this.compression,
        this.contrast,
        this.createDate,
        this.digitalZoomRatio,
        this.exposureCompensation,
        this.exposureMode,
        this.exposureProgram,
        this.exposureTime,
        this.fNumber,
        this.flash,
        this.focalLength,
        this.focalLengthIn35mmFormat,
        this.gainControl,
        this.gpsAltitude,
        this.gpsAltitudeRef,
        this.gpsDateStamp,
        this.gpsDirection,
        this.gpsDirectionRef,
        this.gpsLatitude,
        this.gpsLatitudeRef,
        this.gpsLongitude,
        this.gpsLongitudeRef,
        this.gpsMeasureMode,
        this.gpsSatellites,
        this.gpsStatus,
        this.gpsVersionId,
        this.iso,
        this.lightSource,
        this.make,
        this.meteringMode,
        this.model,
        this.orientation,
        this.saturation,
        this.sceneCaptureType,
        this.sceneType,
        this.sensingMethod,
        this.sharpness,

        // nikon
        this.autoFocusAreaMode,
        this.autoFocusPoint,
        this.activeDLighting,
        this.colorspace,
        this.exposureDifference,
        this.flashColorFilter,
        this.flashCompensation,
        this.flashControlMode,
        this.flashExposureCompensation,
        this.flashFocalLength,
        this.flashMode,
        this.flashSetting,
        this.flashType,
        this.focusDistance,
        this.focusMode,
        this.focusPosition,
        this.highIsoNoiseReduction,
        this.hueAdjustment,
        this.noiseReduction,
        this.pictureControlName,
        this.primaryAFPoint,
        this.vrMode,
        this.vibrationReduction,
        this.vignetteControl,
        this.whiteBalance,

        // composite
        this.aperture,
        this.autoFocus,
        this.depthOfField,
        this.fieldOfView,
        this.hyperfocalDistance,
        this.lensId,
        this.lightValue,
        this.scaleFactor35Efl,
        this.shutterSpeed
    )
}

fun us.mikeandwan.photos.database.SearchHistory.toDomainSearchHistory(): SearchHistory {
    return SearchHistory(
        this.term,
        this.searchDate
    )
}

fun us.mikeandwan.photos.database.SearchPreference.toDomainSearchPreference(): SearchPreference {
    return SearchPreference(
        this.id,
        this.recentQueryCount,
        this.displayType,
        this.gridThumbnailSize
    )
}

fun us.mikeandwan.photos.api.SearchResultCategory.toDomainSearchResult(): SearchResultCategory {
    return SearchResultCategory(
        this.solrId,
        this.id,
        this.year,
        this.name,
        this.multimediaType,
        this.teaserPhotoHeight,
        this.teaserPhotoWidth,
        "${ Constants.WWW_BASE_URL}${this.teaserPhotoPath}",
        this.teaserPhotoSqHeight,
        this.teaserPhotoSqWidth,
        "${ Constants.WWW_BASE_URL}${this.teaserPhotoSqPath}",
        "${ Constants.WWW_BASE_URL}${this.teaserPhotoPath.replace("/xs/", "/md/")}",
        this.score
    )
}

fun us.mikeandwan.photos.api.ApiResult.Error.isUnauthorized(): Boolean {
    return this.errorCode == HttpURLConnection.HTTP_UNAUTHORIZED
}
