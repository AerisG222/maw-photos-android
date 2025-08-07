package us.mikeandwan.photos.ui

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import java.io.File
import kotlin.uuid.Uuid

sealed class MediaListState {
    data object Loading: MediaListState()

    data class CategoryLoaded(
        val category: Category
    ): MediaListState()

    data class Loaded(
        val category: Category,
        val media: List<Media>,
        val activeId: Uuid,
        val activeIndex: Int,
        val activeMedia: Media?,
        val isSlideshowPlaying: Boolean,
        val showDetailSheet: Boolean,
        val setActiveIndex: (index: Int) -> Unit,
        val toggleSlideshow: () -> Unit,
        val toggleFavorite: () -> Unit,
        val toggleDetails: () -> Unit,
        val saveMediaToShare: (drawable: Drawable, filename: String, onComplete: (file: File) -> Unit) -> Unit
    ): MediaListState()
}

@Composable
fun rememberMediaListState(
    category: Category?,
    media: List<Media>,
    activeId: Uuid,
    activeIndex: Int,
    activeMedia: Media?,
    isSlideshowPlaying: Boolean,
    showDetailSheet: Boolean,
    setActiveIndex: (index: Int) -> Unit,
    toggleSlideshow: () -> Unit,
    toggleFavorite: () -> Unit,
    toggleDetails: () -> Unit,
    saveMediaToShare: (drawable: Drawable, filename: String, onComplete: (file: File) -> Unit) -> Unit
): MediaListState {
    if(category == null) {
        return MediaListState.Loading
    }

    if(media.isEmpty() || activeIndex < 0) {
        return MediaListState.CategoryLoaded(category)
    }

    return MediaListState.Loaded(
        category,
        media,
        activeId,
        activeIndex,
        activeMedia,
        isSlideshowPlaying,
        showDetailSheet,
        setActiveIndex,
        toggleSlideshow,
        toggleFavorite,
        toggleDetails,
        saveMediaToShare
    )
}
