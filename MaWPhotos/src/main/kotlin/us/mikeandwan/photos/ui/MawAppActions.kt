package us.mikeandwan.photos.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import us.mikeandwan.photos.domain.models.MediaFeedSubject
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

    /**
     * Marks which person, clan or place the rail is currently showing, or null while the listing
     * they are chosen from is what is on screen.
     */
    fun setActiveFeedSubject(subject: MediaFeedSubject?)

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

    fun navigateToPeople()

    /** Opens the place tree where it was last left, the way the rail's other entries do. */
    fun navigateToPlaces()

    /**
     * Moves to one level of the place tree, the root when [placeId] is null.
     *
     * A level already on the stack is unwound to rather than pushed again, which is what makes the
     * breadcrumb a way back rather than a way deeper.
     */
    fun navigateToPlace(placeId: Uuid?)

    fun navigateToMediaFeed(subject: MediaFeedSubject)

    fun navigateToMediaFeedItem(
        subject: MediaFeedSubject,
        mediaId: Uuid,
    )

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
