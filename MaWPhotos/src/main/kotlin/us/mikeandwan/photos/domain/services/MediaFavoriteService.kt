package us.mikeandwan.photos.domain.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject

class MediaFavoriteService @Inject constructor (
    private val mediaRepository: MediaRepository
){
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    suspend fun setIsFavorite(media: Media, isFavorite: Boolean): Boolean {
        var success = false

        mediaRepository.setFavorite(media.id, isFavorite)
            .collect {
                if (it is ExternalCallStatus.Success) {
                    _isFavorite.value = isFavorite
                    success = true
                }
            }

        return if (success) isFavorite else media.isFavorite

    }
}
