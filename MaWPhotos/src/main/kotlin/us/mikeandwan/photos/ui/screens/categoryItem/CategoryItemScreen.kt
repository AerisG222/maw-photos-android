package us.mikeandwan.photos.ui.screens.categoryItem

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.datasource.HttpDataSource
import java.io.File
import kotlin.uuid.Uuid
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.mediapager.ButtonBar
import us.mikeandwan.photos.ui.components.mediapager.MediaPager
import us.mikeandwan.photos.ui.components.mediapager.OverlayPositionCount
import us.mikeandwan.photos.ui.components.mediapager.rememberRotation
import us.mikeandwan.photos.ui.components.metadata.DetailBottomSheet
import us.mikeandwan.photos.ui.components.metadata.rememberCommentState
import us.mikeandwan.photos.ui.components.metadata.rememberExifState
import us.mikeandwan.photos.ui.components.metadata.rememberWhereState
import us.mikeandwan.photos.ui.components.metadata.rememberWhoState
import us.mikeandwan.photos.ui.components.scaffolds.ItemPagerScaffold
import us.mikeandwan.photos.ui.shared.shareMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItemScreen(
    uiState: CategoryItemUiState,
    videoPlayerDataSourceFactory: HttpDataSource.Factory,
    onSetActiveId: (Uuid) -> Unit,
    onToggleSlideshow: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFaceHighlights: () -> Unit,
    onToggleDetails: () -> Unit,
    onFetchExif: () -> Unit,
    onFetchComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onFetchFaces: () -> Unit,
    onFetchPlaces: () -> Unit,
    onSelectPerson: (Uuid) -> Unit,
    onSelectPlace: (Uuid) -> Unit,
    onSaveMediaToShare: (String, (File) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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

    val whoState = rememberWhoState(
        faces = uiState.mediaFaces,
        fetchFaces = onFetchFaces,
        onSelectPerson = onSelectPerson,
    )

    val whereState = rememberWhereState(
        places = uiState.places,
        fetchPlaces = onFetchPlaces,
        onSelectPlace = onSelectPlace,
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
                    showFaceHighlights = uiState.showFaceHighlights,
                    canHighlightFaces = uiState.canHighlightFaces,
                    onRotateLeft = { rotationState.setActiveRotation(-90f) },
                    onRotateRight = { rotationState.setActiveRotation(90f) },
                    onToggleFaceHighlights = onToggleFaceHighlights,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSlideshow = onToggleSlideshow,
                    onShare = { shareMedia(context, onSaveMediaToShare, activeMedia) },
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
                    whoState = whoState,
                    whereState = whereState,
                    canShowWho = uiState.canHighlightFaces,
                    onDismissRequest = onToggleDetails,
                )
            }
        },
        modifier = modifier,
    ) {
        MediaPager(
            media = uiState.media,
            activeId = uiState.activeId,
            videoPlayerDataSourceFactory = videoPlayerDataSourceFactory,
            setActiveId = onSetActiveId,
            activeRotation = rotationState.activeRotation,
            faces = uiState.faces,
            onZoomChanged = onZoomChanged,
        )
    }
}
