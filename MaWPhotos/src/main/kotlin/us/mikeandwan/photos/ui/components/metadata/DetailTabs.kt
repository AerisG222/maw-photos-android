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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType

/**
 * The cards the details sheet offers, in the order they are laid out.
 *
 * Which of them are on offer depends on the media, so the pager is indexed through this list rather
 * than by a fixed number per card - a video has no Who card, and its Where card is still the last
 * one along.
 */
private enum class DetailTab {
    Comment,
    Exif,
    Who,
    Where,
}

@Composable
fun DetailTabs(
    activeMedia: Media,
    exifState: ExifState,
    commentState: CommentState,
    whoState: WhoState,
    whereState: WhereState,
    // false when the API would refuse the calls behind the card, the same rule the pager's face
    // button follows - a tab that could only ever be empty is not worth offering
    canShowWho: Boolean,
    modifier: Modifier = Modifier,
) {
    // faces are detected on stills, so a video has nobody to list rather than nobody in it
    val showWho = canShowWho && activeMedia.type == MediaType.Photo

    val tabs = remember(showWho) {
        DetailTab.entries.filter { it != DetailTab.Who || showWho }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val (commentMediaId, setCommentMediaId) = remember { mutableStateOf(Uuid.NIL) }
    val (exifMediaId, setExifMediaId) = remember { mutableStateOf(Uuid.NIL) }
    val (whoMediaId, setWhoMediaId) = remember { mutableStateOf(Uuid.NIL) }
    val (whereMediaId, setWhereMediaId) = remember { mutableStateOf(Uuid.NIL) }

    // every card is read only once its own tab is reached, so opening the sheet costs one call
    // rather than four
    LaunchedEffect(activeMedia.id, tabs, pagerState.currentPage) {
        when (tabs.getOrNull(pagerState.currentPage)) {
            DetailTab.Comment -> {
                if (activeMedia.id != commentMediaId) {
                    setCommentMediaId(activeMedia.id)
                    commentState.fetchComments()
                }
            }

            DetailTab.Exif -> {
                if (activeMedia.id != exifMediaId) {
                    setExifMediaId(activeMedia.id)
                    exifState.fetchExif()
                }
            }

            DetailTab.Who -> {
                if (activeMedia.id != whoMediaId) {
                    setWhoMediaId(activeMedia.id)
                    whoState.fetchFaces()
                }
            }

            DetailTab.Where -> {
                if (activeMedia.id != whereMediaId) {
                    setWhereMediaId(activeMedia.id)
                    whereState.fetchPlaces()
                }
            }

            null -> {}
        }
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index

                Tab(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = tab.iconId()),
                            contentDescription = stringResource(id = tab.labelId()),
                            modifier = Modifier.size(32.dp),
                            tint = if (selected) activeColor else inactiveColor,
                        )
                    },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            pageContent = { page ->
                when (tabs.getOrNull(page)) {
                    DetailTab.Comment -> {
                        CommentScreen(commentState, modifier = Modifier.fillMaxSize())
                    }

                    DetailTab.Exif -> {
                        ExifScreen(exifState, modifier = Modifier.fillMaxSize())
                    }

                    DetailTab.Who -> {
                        WhoScreen(whoState, modifier = Modifier.fillMaxSize())
                    }

                    DetailTab.Where -> {
                        WhereScreen(whereState, modifier = Modifier.fillMaxSize())
                    }

                    null -> {}
                }
            },
        )
    }
}

private fun DetailTab.iconId(): Int =
    when (this) {
        DetailTab.Comment -> R.drawable.ic_comment_white
        DetailTab.Exif -> R.drawable.ic_tune
        DetailTab.Who -> R.drawable.ic_people
        DetailTab.Where -> R.drawable.ic_place
    }

private fun DetailTab.labelId(): Int =
    when (this) {
        DetailTab.Comment -> R.string.media_detail_comment_tab_description
        DetailTab.Exif -> R.string.media_detail_exif_tab_description
        DetailTab.Who -> R.string.media_detail_who_tab_description
        DetailTab.Where -> R.string.media_detail_where_tab_description
    }

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DetailTabsPreview() {
    DetailTabs(
        activeMedia = Media(
            id = Uuid.random(),
            categoryId = Uuid.random(),
            type = MediaType.Photo,
            isFavorite = false,
            files = emptyList(),
        ),
        exifState = ExifState(null) {},
        commentState = CommentState(emptyList(), {}, {}),
        whoState = WhoState(null, {}, {}),
        whereState = WhereState(null, {}, {}),
        canShowWho = true,
    )
}
