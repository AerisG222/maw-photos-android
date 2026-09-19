package us.mikeandwan.photos.domain.models

data class MediaPreference(
    val slideshowIntervalSeconds: Int = 3,
    val showFaceHighlights: Boolean = false,
)
