package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.mikeandwan.photos.domain.models.Media

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    sheetState: SheetState,
    activeMedia: Media,
    exifState: ExifState,
    commentState: CommentState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        onDismissRequest = { onDismissRequest() },
        modifier = modifier,
    ) {
        DetailTabs(
            activeMedia = activeMedia,
            exifState = exifState,
            commentState = commentState,
        )
    }
}
