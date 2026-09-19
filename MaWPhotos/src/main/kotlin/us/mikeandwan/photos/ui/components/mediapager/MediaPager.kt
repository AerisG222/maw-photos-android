package us.mikeandwan.photos.ui.components.mediapager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.media3.datasource.HttpDataSource
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlin.uuid.Uuid
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import us.mikeandwan.photos.domain.models.FaceHighlight
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.videoplayer.VideoPlayer
import us.mikeandwan.photos.ui.shared.getMediaUrl

@Composable
fun MediaPager(
    media: List<Media>,
    activeId: Uuid,
    videoPlayerDataSourceFactory: HttpDataSource.Factory,
    setActiveId: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    activeRotation: Float = 0f,
    // the faces in the active item only.  the pager keeps neighbours composed so a swipe is
    // instant, and fetching for pages nobody has landed on would spend calls on faces that are
    // never seen.
    faces: List<FaceHighlight> = emptyList(),
    // whether the photo being looked at is zoomed in.  only ever reported for the active page, and
    // always handed back as false before that page goes, so a zoom cannot be left behind reported
    onZoomChanged: (Boolean) -> Unit = {},
) {
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    val pagerState = rememberPagerState(
        pageCount = { media.size },
        initialPage = media.indexOfFirst { it.id == activeId },
    )

    // Use settledPage so this only fires after a page fully lands, not during the animation.
    // currentPage emits intermediate values during programmatic animations, which would call
    // setActiveId mid-animation and restart LaunchedEffect(activeId), creating a feedback loop.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page >= 0 && page < media.size) {
                setActiveId(media[page].id)
            }
        }
    }

    LaunchedEffect(activeId) {
        val targetIndex = media.indexOfFirst { it.id == activeId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(0.dp),
        userScrollEnabled = true,
        modifier = modifier.fillMaxSize(),
    ) { index ->
        val activeMedia = media[index]

        val swipeAnimation = Modifier
            .graphicsLayer {
                val pageOffset =
                    (pagerState.currentPage - index) +
                        pagerState.currentPageOffsetFraction

                alpha = lerp(
                    start = 0.4f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f),
                )

                cameraDistance = 8 * density
                rotationY = lerp(
                    start = 0f,
                    stop = 40f,
                    fraction = pageOffset.coerceIn(-1f, 1f),
                )

                lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f),
                ).also { scale ->
                    scaleX = scale
                    scaleY = scale
                }
            }

        when (activeMedia.type) {
            MediaType.Photo -> {
                // the photo's own pixel size, which the overlay needs to work out where a
                // normalised face box lands once ContentScale.Fit has letterboxed it.  unknown
                // until the image has loaded, and forgotten when the page is reused for another.
                var imageSize by remember(activeMedia.id) { mutableStateOf(Size.Unspecified) }

                // one per page, so a zoom belongs to the photo it was made on and is gone once that
                // page has been swiped away and disposed
                val zoomState = rememberZoomState()
                val isZoomed by remember(zoomState) { derivedStateOf { zoomState.scale > 1f } }

                // the key of the screen-sized bitmap, which stands in while the full-size one is
                // decoded so zooming never flashes an empty page
                var previewKey by remember(activeMedia.id) { mutableStateOf<MemoryCache.Key?>(null) }

                val model = rememberPhotoModel(activeMedia.getMediaUrl(), isZoomed, previewKey)

                // keyed on the combined value, so the effect ending - whether because the zoom was
                // let go, another page became active, or this one was disposed - is what reports it
                val isZoomedActivePage = isZoomed && activeMedia.id == activeId

                DisposableEffect(isZoomedActivePage) {
                    if (isZoomedActivePage) {
                        currentOnZoomChanged(true)
                    }

                    onDispose {
                        if (isZoomedActivePage) {
                            currentOnZoomChanged(false)
                        }
                    }
                }

                // zoom and rotation move the box rather than the image inside it, so the overlay is
                // carried along by the same transforms instead of tracking them itself
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(
                            zoomState,
                            scrollGesturePropagation = ScrollGesturePropagation.NotZoomed,
                        )
                        .then(swipeAnimation)
                        .rotate(activeRotation),
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        onSuccess = {
                            imageSize = it.painter.intrinsicSize
                            // only the screen-sized decode is written to the memory cache, so only
                            // it has a key to remember
                            it.result.memoryCacheKey?.let { key -> previewKey = key }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (activeMedia.id == activeId && faces.isNotEmpty()) {
                        FaceHighlightOverlay(
                            faces = faces,
                            imageSize = imageSize,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            MediaType.Video -> {
                VideoPlayer(
                    activeMedia,
                    videoPlayerDataSourceFactory,
                    Modifier
                        .fillMaxSize()
                        .then(swipeAnimation),
                )
            }
        }
    }
}

/**
 * What to load for a photo in the pager: the url as it is while the photo is shown at its fitted
 * size, and a full-size decode only while it is zoomed.
 *
 * Coil decodes to the size of what it is drawn into, which for the pager is the screen - enough
 * until somebody zooms, at which point it is being stretched.  Decoding every photo at full size
 * would keep zoom sharp, but it multiplies the memory of every photo viewed, zoomed or not, and
 * bitmap memory is what android's per-app memory limits and play's memory requirements measure.
 * So the full-size bitmap is only made for the photo actually being zoomed, and it is kept out of
 * the memory cache: once the photo is zoomed back out or swiped away, nothing holds on to it.
 */
@Composable
private fun rememberPhotoModel(
    url: String,
    isZoomed: Boolean,
    previewKey: MemoryCache.Key?,
): Any {
    val context = LocalContext.current

    return remember(url, isZoomed, previewKey) {
        if (isZoomed) {
            ImageRequest
                .Builder(context)
                .data(url)
                .size(coil3.size.Size.ORIGINAL)
                // read so the placeholder can come from the cache, never written - see above
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .placeholderMemoryCacheKey(previewKey)
                .build()
        } else {
            url
        }
    }
}
