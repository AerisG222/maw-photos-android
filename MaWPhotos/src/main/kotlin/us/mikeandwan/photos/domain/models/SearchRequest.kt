package us.mikeandwan.photos.domain.models

data class SearchRequest(
    val query: String,
    val source: SearchSource,
)
