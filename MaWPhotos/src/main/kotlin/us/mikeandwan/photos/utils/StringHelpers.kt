package us.mikeandwan.photos.utils

fun getFilenameFromUrl(url: String): String = url.substringAfterLast('/')
