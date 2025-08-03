package us.mikeandwan.photos.ui.controls.videoplayer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.getMediaUrl

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    activeMedia: Media,
    videoPlayerHttpDataSourceFactory: HttpDataSource.Factory,
    modifier: Modifier = Modifier
) {
    if(activeMedia.type != MediaType.Video) {
        return
    }

    val context = LocalContext.current
    val exoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(videoPlayerHttpDataSourceFactory)
        )
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(activeMedia.getMediaUrl()))
            prepare()
            //playWhenReady = true
        }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                player = exoPlayer
            }
        }
    )
}
