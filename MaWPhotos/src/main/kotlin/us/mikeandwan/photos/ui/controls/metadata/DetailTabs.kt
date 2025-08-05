package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Media
import kotlin.uuid.Uuid

private object TabIndex {
    const val COMMENT = 0
    const val EXIF = 1
}

@Composable
fun DetailTabs(
    activeMedia: Media,
    exifState: ExifState,
    commentState: CommentState
) {
    val tabs = listOf(TabIndex.COMMENT, TabIndex.EXIF)

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val (commentMediaId, setCommentMediaId) = remember { mutableStateOf(Uuid.NIL) }
    val (exifMediaId, setExifMediaId) = remember { mutableStateOf(Uuid.NIL) }

    val bgActive = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    val bgInactive = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = pagerState.currentPage == TabIndex.COMMENT,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(TabIndex.COMMENT)
                    }
                },
                icon = {
                    AsyncImage(
                        model = R.drawable.ic_comment_white,
                        contentDescription = "Comment",
                        modifier = Modifier.size(32.dp),
                        colorFilter = if(pagerState.currentPage == TabIndex.COMMENT) { bgActive } else { bgInactive }
                    )
                }
            )

            if(tabs.contains(TabIndex.EXIF)) {
                Tab(
                    selected = pagerState.currentPage == TabIndex.EXIF,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(TabIndex.EXIF)
                        }
                    },
                    icon = {
                        AsyncImage(
                            model = R.drawable.ic_tune,
                            contentDescription = "Exif",
                            modifier = Modifier.size(32.dp),
                            colorFilter = if(pagerState.currentPage == TabIndex.EXIF) { bgActive } else { bgInactive }
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            pageContent = {
                when(it) {
                    TabIndex.COMMENT -> {
                        if(activeMedia.id != commentMediaId) {
                            setCommentMediaId(activeMedia.id)
                            commentState.fetchComments()
                        }

                        CommentScreen(commentState)
                    }
                    TabIndex.EXIF -> {
                        if(activeMedia.id != exifMediaId) {
                            setExifMediaId(activeMedia.id)
                            exifState.fetchExif()
                        }

                        ExifScreen(exifState)
                    }
                }
            }
        )
    }
}
