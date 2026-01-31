package us.mikeandwan.photos.domain.models

import java.util.Calendar

data class SearchHistory(
    val term: String,
    val searchDate: Calendar,
)
