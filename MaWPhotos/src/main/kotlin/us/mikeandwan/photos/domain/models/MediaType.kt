package us.mikeandwan.photos.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    Photo,
    Video,
}
