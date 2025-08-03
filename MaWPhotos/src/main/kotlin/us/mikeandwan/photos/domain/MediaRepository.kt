package us.mikeandwan.photos.domain

import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject

class MediaRepository @Inject constructor (
    private val photoRepository: PhotoRepository
) {
    fun getExifData(media: Media) =
        photoRepository.getExifData(media.id)

    fun getRating(media: Media) =
        photoRepository.getRating(media.id)

    fun getComments(media: Media) =
        photoRepository.getComments(media.id)

    fun addComment(media: Media, comment: String) =
        photoRepository.addComment(media.id, comment)

    fun setRating(media: Media, rating: Short) =
        photoRepository.setRating(media.id, rating)
}
