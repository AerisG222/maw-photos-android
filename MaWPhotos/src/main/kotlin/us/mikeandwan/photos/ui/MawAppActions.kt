package us.mikeandwan.photos.ui

import androidx.compose.runtime.staticCompositionLocalOf
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.components.topbar.TopBarState

interface MawAppActions {
    fun updateTopBar(state: TopBarState)
    fun setNavArea(area: NavigationArea)
}

val LocalMawAppActions = staticCompositionLocalOf<MawAppActions> {
    error("No MawAppActions provided")
}
