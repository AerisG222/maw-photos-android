package us.mikeandwan.photos.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import us.mikeandwan.photos.R
import us.mikeandwan.photos.ui.theme.MawGlanceColorProviders

class RandomPhotoWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val IMAGE_URL_KEY = stringPreferencesKey("random_photo_url")
        val CATEGORY_NAME_KEY = stringPreferencesKey("category_name")
        val CATEGORY_YEAR_KEY = intPreferencesKey("category_year")
        val SHOW_INFO_KEY = booleanPreferencesKey("show_info")
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            val imageUrl = prefs[IMAGE_URL_KEY]
            val categoryName = prefs[CATEGORY_NAME_KEY]
            val categoryYear = prefs[CATEGORY_YEAR_KEY]
            val showInfo = prefs[SHOW_INFO_KEY] ?: true

            val bitmap by produceState<Bitmap?>(initialValue = null, key1 = imageUrl) {
                value = if (imageUrl != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val file = File(imageUrl)
                            if (file.exists()) {
                                Timber.d("Loading bitmap from $imageUrl, size: ${file.length()}")
                                val options = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                BitmapFactory.decodeFile(imageUrl, options)
                            } else {
                                Timber.e("File does not exist at $imageUrl")
                                null
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error decoding bitmap from $imageUrl")
                            null
                        }
                    }
                } else {
                    Timber.d("imageUrl is null in preferences")
                    null
                }
            }

            GlanceTheme(colors = MawGlanceColorProviders) {
                RandomPhotoWidgetContent(
                    bitmap = bitmap,
                    isError = imageUrl != null && bitmap == null,
                    categoryName = categoryName,
                    categoryYear = categoryYear,
                    showInfo = showInfo,
                )
            }
        }
    }

    @Composable
    private fun RandomPhotoWidgetContent(
        bitmap: Bitmap?,
        isError: Boolean,
        categoryName: String?,
        categoryYear: Int?,
        showInfo: Boolean,
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
                        modifier = GlanceModifier.fillMaxSize(),
                    )

                    if (showInfo && categoryName != null && categoryYear != null) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Column(
                                modifier = GlanceModifier
                                    .background(ImageProvider(R.drawable.bg_metadata_widget))
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = categoryName,
                                    modifier = GlanceModifier,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = categoryYear.toString(),
                                    modifier = GlanceModifier,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                isError -> {
                    Text(
                        text = "Error loading image",
                        modifier = GlanceModifier,
                    )
                }

                else -> {
                    Text(
                        text = "Loading...",
                        modifier = GlanceModifier,
                    )
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.bg_shuffle_widget))
                        .clickable(actionRunCallback<RefreshWidgetAction>())
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_shuffle),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    )
                }
            }
        }
    }
}
