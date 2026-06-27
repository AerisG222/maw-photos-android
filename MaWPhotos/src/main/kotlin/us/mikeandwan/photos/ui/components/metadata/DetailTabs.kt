package us.mikeandwan.photos.ui.components.metadata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Media

private object TabIndex {
    const val COMMENT = 0
    const val EXIF = 1
}

@Composable
fun DetailTabs(
    activeMedia: Media,
    exifState: ExifState,
    commentState: CommentState,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(TabIndex.COMMENT, TabIndex.EXIF)

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val (commentMediaId, setCommentMediaId) = remember { mutableStateOf(Uuid.NIL) }
    val (exifMediaId, setExifMediaId) = remember { mutableStateOf(Uuid.NIL) }

    LaunchedEffect(activeMedia.id, pagerState.currentPage) {
        when (pagerState.currentPage) {
            TabIndex.COMMENT -> {
                if (activeMedia.id != commentMediaId) {
                    setCommentMediaId(activeMedia.id)
                    commentState.fetchComments()
                }
            }

            TabIndex.EXIF -> {
                if (activeMedia.id != exifMediaId) {
                    setExifMediaId(activeMedia.id)
                    exifState.fetchExif()
                }
            }
        }
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Tab(
                selected = pagerState.currentPage == TabIndex.COMMENT,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(TabIndex.COMMENT)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment_white),
                        contentDescription = "Comment",
                        modifier = Modifier.size(32.dp),
                        tint = if (pagerState.currentPage == TabIndex.COMMENT) {
                            activeColor
                        } else {
                            inactiveColor
                        },
                    )
                },
            )

            if (tabs.contains(TabIndex.EXIF)) {
                Tab(
                    selected = pagerState.currentPage == TabIndex.EXIF,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(TabIndex.EXIF)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = "Exif",
                            modifier = Modifier.size(32.dp),
                            tint = if (pagerState.currentPage == TabIndex.EXIF) {
                                activeColor
                            } else {
                                inactiveColor
                            },
                        )
                    },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            pageContent = {
                when (it) {
                    TabIndex.COMMENT -> {
                        CommentScreen(commentState, modifier = Modifier.fillMaxSize())
                    }

                    TabIndex.EXIF -> {
                        ExifScreen(exifState, modifier = Modifier.fillMaxSize())
                    }
                }
            },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DetailTabsPreview() {
    DetailTabs(
        activeMedia = Media(
            id = Uuid.random(),
            categoryId = Uuid.random(),
            type = us.mikeandwan.photos.domain.models.MediaType.Photo,
            isFavorite = false,
            files = emptyList()
        ),
        exifState = ExifState(null) {},
        commentState = CommentState(emptyList(), {}, {})
    )
}
