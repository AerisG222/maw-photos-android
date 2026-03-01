package us.mikeandwan.photos.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import us.mikeandwan.photos.R

class RandomPhotoWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val IMAGE_URL_KEY = stringPreferencesKey("random_photo_url")
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            // 1. Observe the preferences reactively
            val prefs = currentState<Preferences>()
            val imageUrl = prefs[IMAGE_URL_KEY]
            val context = LocalContext.current

            // 2. Manage the bitmap as internal state
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            var isError by remember { mutableStateOf(false) }

            // 3. React to imageUrl changes without polling
            LaunchedEffect(imageUrl) {
                if (imageUrl != null) {
                    val loader = SingletonImageLoader.get(context)
                    val request = ImageRequest
                        .Builder(context)
                        .data(imageUrl)
                        .build()

                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        bitmap = (result.image as? BitmapImage)?.bitmap
                        isError = false
                    } else {
                        isError = true
                    }
                }
            }

            RandomPhotoWidgetContent(bitmap, isError)
        }
    }

    @Composable
    private fun RandomPhotoWidgetContent(
        bitmap: Bitmap?,
        isError: Boolean,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = "Random Photo",
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.fillMaxSize().clickable(
                            actionRunCallback<RefreshWidgetAction>(),
                        ),
                    )
                }

                isError -> {
                    Text(
                        text = "Error loading image",
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>()),
                    )
                }

                else -> {
                    Text(
                        text = "Loading...",
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>()),
                    )
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_shuffle),
                    contentDescription = "Refresh",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                )
            }
        }
    }
}
