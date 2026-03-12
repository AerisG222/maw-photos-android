package us.mikeandwan.photos.ui.screens.categoryItem

import android.graphics.drawable.Drawable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.datasource.HttpDataSource
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.mediapager.ButtonBar
import us.mikeandwan.photos.ui.components.mediapager.MediaPager
import us.mikeandwan.photos.ui.components.mediapager.OverlayPositionCount
import us.mikeandwan.photos.ui.components.mediapager.rememberRotation
import us.mikeandwan.photos.ui.components.metadata.DetailBottomSheet
import us.mikeandwan.photos.ui.components.metadata.rememberCommentState
import us.mikeandwan.photos.ui.components.metadata.rememberExifState
import us.mikeandwan.photos.ui.components.scaffolds.ItemPagerScaffold
import us.mikeandwan.photos.ui.shared.shareMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItemScreen(
    uiState: CategoryItemUiState,
    videoPlayerDataSourceFactory: HttpDataSource.Factory,
    onSetActiveId: (Uuid) -> Unit,
    onToggleSlideshow: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDetails: () -> Unit,
    onFetchExif: () -> Unit,
    onFetchComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onSaveMediaToShare: (Drawable, String, (File) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val rotationState = rememberRotation(uiState.activeId)

    val exifState = rememberExifState(
        exif = uiState.exif,
        fetchExif = onFetchExif,
    )

    val commentState = rememberCommentState(
        comments = uiState.comments,
        fetchComments = onFetchComments,
        addComment = onAddComment,
    )

    if (uiState.isLoading) {
        Loading()
        return
    }

    ItemPagerScaffold(
        showDetails = uiState.showDetailSheet,
        topRightContent = {
            OverlayPositionCount(
                position = uiState.media.indexOfFirst { it.id == uiState.activeId } + 1,
                count = uiState.media.size,
            )
        },
        bottomBarContent = {
            uiState.activeMedia?.let { activeMedia ->
                ButtonBar(
                    activeMediaType = activeMedia.type,
                    isSlideshowPlaying = uiState.isSlideshowPlaying,
                    isFavorite = activeMedia.isFavorite,
                    onRotateLeft = { rotationState.setActiveRotation(-90f) },
                    onRotateRight = { rotationState.setActiveRotation(90f) },
                    onToggleFavorite = onToggleFavorite,
                    onToggleSlideshow = onToggleSlideshow,
                    onShare = {
                        coroutineScope.launch {
                            shareMedia(
                                context,
                                onSaveMediaToShare,
                                activeMedia,
                            )
                        }
                    },
                    onViewDetails = onToggleDetails,
                )
            }
        },
        detailSheetContent = {
            uiState.activeMedia?.let { activeMedia ->
                DetailBottomSheet(
                    activeMedia = activeMedia,
                    sheetState = sheetState,
                    exifState = exifState,
                    commentState = commentState,
                    onDismissRequest = onToggleDetails,
                )
            }
        },
        modifier = modifier,
    ) {
        MediaPager(
            uiState.media,
            activeId = uiState.activeId,
            rotationState.activeRotation,
            videoPlayerDataSourceFactory,
            setActiveId = onSetActiveId,
        )
    }
}
