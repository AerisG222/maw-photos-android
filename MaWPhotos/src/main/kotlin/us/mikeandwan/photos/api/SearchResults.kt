package us.mikeandwan.photos.api

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults<T>(
    var results: List<T> = ArrayList(),
    var hasMoreResults: Boolean,
    var nextOffset: Int
)
