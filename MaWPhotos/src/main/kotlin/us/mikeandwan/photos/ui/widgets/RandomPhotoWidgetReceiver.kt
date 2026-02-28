package us.mikeandwan.photos.ui.widgets

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class RandomPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RandomPhotoWidget()
}
