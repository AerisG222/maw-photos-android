package us.mikeandwan.photos.utils

import android.app.PendingIntent

class PendingIntentFlagHelper {
    companion object {
        fun getMutableFlag(baseFlags: Int): Int = baseFlags or PendingIntent.FLAG_MUTABLE
    }
}
