package us.mikeandwan.photos.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.components.topbar.TopBarState

interface MawAppActions {
    // UI State
    fun updateTopBar(
        area: NavigationArea,
        state: TopBarState,
    )

    fun setNavArea(area: NavigationArea)

    fun setActiveYear(year: Int)

    fun openDrawer()

    fun closeDrawer()

    // Navigation (Generic)
    fun navigate(route: NavKey)

    // Navigation (Specific)
    fun navigateToAbout()

    fun navigateToCategories(year: Int? = null)

    fun navigateToCategory(categoryId: Uuid)

    fun navigateToCategoryItem(
        categoryId: Uuid,
        mediaId: Uuid,
    )

    fun navigateToInactiveUser()

    fun navigateToLogin()

    fun navigateToRandom()

    fun navigateToRandomItem(mediaId: Uuid)

    fun navigateToSearch(searchTerm: String? = null)

    fun navigateToSettings()

    fun navigateToUpload()

    fun back()
}

val LocalMawAppActions = staticCompositionLocalOf<MawAppActions> {
    error("No MawAppActions provided")
}
