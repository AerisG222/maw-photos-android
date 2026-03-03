package us.mikeandwan.photos.ui.navigation

import androidx.navigation3.runtime.NavKey
import us.mikeandwan.photos.ui.screens.about.AboutNavKey
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserNavKey
import us.mikeandwan.photos.ui.screens.login.LoginNavKey
import us.mikeandwan.photos.ui.screens.settings.SettingsNavKey
import us.mikeandwan.photos.ui.screens.upload.UploadNavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(
    val state: NavigationState,
) {
    fun navigate(route: NavKey) {
        if (routeShouldNeverHaveChildren(route) || route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    fun routeShouldNeverHaveChildren(route: NavKey) =
        // this helps handle the case for handling send intents and more generally insuring
        // that none of these routes track children
        when (route)
        {
            is AboutNavKey -> true
            is InactiveUserNavKey -> true
            is LoginNavKey -> true
            is SettingsNavKey -> true
            is UploadNavKey -> true
            else -> false
        }
}
