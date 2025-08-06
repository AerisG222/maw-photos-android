package us.mikeandwan.photos.domain.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Comment
import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject

class MediaCommentService @Inject constructor (
    private val mediaRepository: MediaRepository,
) {
    private val _comments = MutableStateFlow(emptyList<Comment>())
    val comments = _comments.asStateFlow()

    suspend fun fetchCommentDetails(media: Media) {
        mediaRepository.getComments(media.id)
            .map {
                when (it) {
                    is ExternalCallStatus.Loading -> emptyList()
                    is ExternalCallStatus.Error -> emptyList()
                    is ExternalCallStatus.Success -> it.result
                }
            }
            .collect { _comments.value = it }
    }

    suspend fun addComment(media: Media, comment: String) {
        if(comment.isNotBlank()) {
            mediaRepository.addComment(media.id, comment)
                .collect { result ->
                    if (result is ExternalCallStatus.Success) {
                        _comments.update { list ->
                            list + result.result
                        }
                    }
                }
        }
    }
}
