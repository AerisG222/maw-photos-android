package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaPreference(
    val slideshowIntervalSeconds: Int = 3,
    // one setting rather than one per view: somebody who wants faces marked generally wants them
    // marked wherever media is shown.  off by default, because nothing about faces is fetched until
    // it is on.
    val showFaceHighlights: Boolean = false,
)
