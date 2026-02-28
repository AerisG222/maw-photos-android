package us.mikeandwan.photos.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult

class RandomPhotoWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val IMAGE_URL_KEY = stringPreferencesKey("random_photo_url")
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val prefs = getAppWidgetState(context, stateDefinition, id) as Preferences
        val imageUrl = prefs[IMAGE_URL_KEY]

        var bitmap: Bitmap? = null
        var isError = false

        if (imageUrl != null) {
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest
                .Builder(context)
                .data(imageUrl)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                bitmap = (result.image as? BitmapImage)?.bitmap
            } else {
                isError = true
            }
        }

        provideContent {
            RandomPhotoWidgetContent(bitmap, isError)
        }
    }

    @Composable
    private fun RandomPhotoWidgetContent(
        bitmap: Bitmap?,
        isError: Boolean,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = "Random Photo",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                }

                isError -> {
                    Text(text = "Error loading image")
                }

                else -> {
                    Text(text = "Loading...")
                }
            }
        }
    }
}
