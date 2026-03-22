package us.mikeandwan.photos.ui.screens.upload

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.uuid.Uuid
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.ui.components.mediagrid.MediaGrid
import us.mikeandwan.photos.ui.components.mediagrid.MediaGridItem
import us.mikeandwan.photos.ui.components.mediagrid.rememberMediaGridState

@Composable
fun UploadScreen(
    uiState: UploadUiState,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberMediaGridState(
        gridItems = uiState.filesToUpload.mapIndexed { _, file ->
            MediaGridItem(Uuid.random(), file.path, emptyList(), file)
        },
        thumbnailSize = GridThumbnailSize.Medium,
        onSelectGridItem = { },
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        AsyncImage(
            model = R.drawable.ic_share,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            contentDescription = stringResource(id = R.string.share_photo_icon_description),
            modifier = Modifier
                .padding(40.dp)
                .fillMaxSize(),
        )
    }

    Box(modifier = modifier) {
        MediaGrid(gridState, modifier = modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun UploadScreenEmptyPreview() {
    UploadScreen(
        uiState = UploadUiState(filesToUpload = emptyList(), isLoading = false)
    )
}
