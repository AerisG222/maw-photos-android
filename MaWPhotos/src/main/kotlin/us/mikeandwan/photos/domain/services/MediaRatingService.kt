package us.mikeandwan.photos.domain.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject

class MediaRatingService @Inject constructor (
    private val mediaRepository: MediaRepository
){
    private val _isFavorite = MutableStateFlow<Boolean>(false)
    val isFavorite = _isFavorite.asStateFlow()

    suspend fun setIsFavorite(media: Media, isFavorite: Boolean) {
        mediaRepository.setFavorite(media.id, isFavorite)
            .collect { }

        _isFavorite.value = isFavorite
    }
}
