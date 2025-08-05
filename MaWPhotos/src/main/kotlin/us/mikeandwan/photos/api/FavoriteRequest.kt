package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRequest(
    val isFavorite: Boolean
)
