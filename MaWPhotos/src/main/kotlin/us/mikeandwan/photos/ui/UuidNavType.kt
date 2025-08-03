package us.mikeandwan.photos.ui

import android.os.Bundle
import androidx.navigation.NavType
import kotlin.uuid.Uuid

val UuidNavType = object : NavType<Uuid?>(isNullableAllowed = true) {
    override val name: String
        get() = "Uuid"

    override fun put(bundle: Bundle, key: String, value: Uuid?) {
        bundle.putString(key, value?.toString())
    }

    @Suppress("DEPRECATION") // For older Android versions if not using Android Tiramisu APIs
    override fun get(bundle: Bundle, key: String): Uuid? {
        return bundle.getString(key)?.let {
            try {
                Uuid.parse(it)
            } catch (e: IllegalArgumentException) {
                // Handle malformed Uuid string if necessary, or let it crash
                // to indicate a programming error during navigation.
                null
            }
        }
    }

    override fun parseValue(value: String): Uuid? {
        return try {
            Uuid.parse(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // Only needed if supporting default values directly in the navArgument block
    // and your default value is a Uuid instance. Not strictly necessary if you
    // always provide the argument or your default is null.
    override fun serializeAsValue(value: Uuid?): String {
        return value?.toString() ?: "null" // Or handle null differently
    }
}
