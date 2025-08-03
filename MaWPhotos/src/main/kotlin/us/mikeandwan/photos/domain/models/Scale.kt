package us.mikeandwan.photos.domain.models

data class Scale(
    val code: String,
    val width: Int,
    val height: Int,
    val fillsDimensions: Boolean
)
