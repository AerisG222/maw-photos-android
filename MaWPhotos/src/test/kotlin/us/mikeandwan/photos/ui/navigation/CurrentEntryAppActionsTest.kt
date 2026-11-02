package us.mikeandwan.photos.ui.navigation

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.MawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

class CurrentEntryAppActionsTest {
    private val app = mockk<MawAppActions>(relaxed = true)
    private val topBar = TopBarState(title = "2026")
    private var isCurrent = true
    private val actions = CurrentEntryAppActions(app) { isCurrent }

    @Test
    fun `the current entry writes straight through`() {
        actions.setNavArea(NavigationArea.Category)
        actions.updateTopBar(NavigationArea.Category, topBar)

        verify { app.setNavArea(NavigationArea.Category) }
        verify { app.updateTopBar(NavigationArea.Category, topBar) }
    }

    // the entry under a back gesture, which is composed but may never become current
    @Test
    fun `an entry being previewed changes nothing`() {
        isCurrent = false

        actions.setNavArea(NavigationArea.Category)
        actions.setActiveYear(2026)
        actions.setActiveFeedSubject(null)
        actions.updateTopBar(NavigationArea.Category, topBar)

        verify(exactly = 0) { app.setNavArea(any()) }
        verify(exactly = 0) { app.setActiveYear(any()) }
        verify(exactly = 0) { app.setActiveFeedSubject(any()) }
        verify(exactly = 0) { app.updateTopBar(any(), any()) }
    }

    // its effects have already run by the time the gesture completes, and will not run again
    @Test
    fun `what a previewed entry asked for applies when it becomes current, area first`() {
        isCurrent = false

        actions.updateTopBar(NavigationArea.Category, topBar)
        actions.setActiveFeedSubject(null)
        actions.setNavArea(NavigationArea.Category)

        isCurrent = true
        actions.applyHeld()

        verifyOrder {
            app.setNavArea(NavigationArea.Category)
            app.setActiveFeedSubject(null)
            app.updateTopBar(NavigationArea.Category, topBar)
        }
    }

    @Test
    fun `becoming current having asked for nothing changes nothing`() {
        actions.applyHeld()

        verify(exactly = 0) { app.setNavArea(any()) }
        verify(exactly = 0) { app.setActiveFeedSubject(any()) }
        verify(exactly = 0) { app.updateTopBar(any(), any()) }
    }
}
