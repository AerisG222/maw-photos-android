package us.mikeandwan.photos.utils

import androidx.core.net.toUri

fun getFilenameFromUrl(url: String): String = url.toUri().lastPathSegment ?: url.substringAfterLast('/')
