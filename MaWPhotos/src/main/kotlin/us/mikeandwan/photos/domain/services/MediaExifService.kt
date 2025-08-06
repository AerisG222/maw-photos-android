package us.mikeandwan.photos.domain.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject

class MediaExifService @Inject constructor (
    private val mediaRepository: MediaRepository
) {
    private val _exif = MutableStateFlow<JsonElement?>(null)
    val exif = _exif.asStateFlow()

    suspend fun fetchExifDetails(media: Media) {
        _exif.value = mediaRepository.getExifData(media.id)
            .filterIsInstance<ExternalCallStatus.Success<JsonElement>>()
            .map { it.result }
            .firstOrNull()
    }
}
