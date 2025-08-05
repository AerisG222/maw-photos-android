package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.runtime.Composable

class RatingState(
    val isFavorite: Boolean,
    val updateIsFavorite: (isFavorite: Boolean) -> Unit
)

@Composable
fun rememberRatingState(
    isFavorite: Boolean = false,
    updateIsFavorite: (isFavorite: Boolean) -> Unit = {}
) : RatingState {
    return RatingState(
        isFavorite = isFavorite,
        updateIsFavorite = updateIsFavorite
    )
}
