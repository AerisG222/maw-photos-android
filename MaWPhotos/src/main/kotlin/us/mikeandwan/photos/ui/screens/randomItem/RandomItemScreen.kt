package us.mikeandwan.photos.ui.screens.randomItem

import android.graphics.drawable.Drawable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.datasource.HttpDataSource
import java.io.File
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.mediapager.ButtonBar
import us.mikeandwan.photos.ui.components.mediapager.MediaPager
import us.mikeandwan.photos.ui.components.mediapager.OverlayPositionCount
import us.mikeandwan.photos.ui.components.mediapager.OverlayYearName
import us.mikeandwan.photos.ui.components.mediapager.rememberRotation
import us.mikeandwan.photos.ui.components.metadata.DetailBottomSheet
import us.mikeandwan.photos.ui.components.metadata.rememberCommentState
import us.mikeandwan.photos.ui.components.metadata.rememberExifState
import us.mikeandwan.photos.ui.components.scaffolds.ItemPagerScaffold
import us.mikeandwan.photos.ui.shared.shareMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomItemScreen(
    uiState: RandomItemUiState,
    videoPlayerDataSourceFactory: HttpDataSource.Factory,
    onNavigateToYear: (Int) -> Unit,
    onNavigateToCategory: (Category) -> Unit,
    onSetActiveIndex: (Int) -> Unit,
    onToggleSlideshow: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDetails: () -> Unit,
    onFetchExif: () -> Unit,
    onFetchComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onSaveMediaToShare: (Drawable, String, (File) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Loading()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val rotationState = rememberRotation(uiState.activeIndex)

    val exifState = rememberExifState(
        exif = uiState.exif,
        fetchExif = onFetchExif,
    )

    val commentState = rememberCommentState(
        comments = uiState.comments,
        fetchComments = onFetchComments,
        addComment = onAddComment,
    )

    ItemPagerScaffold(
        showDetails = uiState.showDetailSheet,
        topLeftContent = {
            uiState.category?.let {
                OverlayYearName(
                    category = it,
                    onClickYear = onNavigateToYear,
                    onClickCategory = onNavigateToCategory,
                )
            }
        },
        topRightContent = {
            OverlayPositionCount(
                position = uiState.activeIndex + 1,
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
            uiState.activeIndex,
            rotationState.activeRotation,
            videoPlayerDataSourceFactory,
            setActiveIndex = onSetActiveIndex,
        )
    }
}
