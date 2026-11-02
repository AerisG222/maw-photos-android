package us.mikeandwan.photos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.MawAppActions
import us.mikeandwan.photos.ui.components.topbar.TopBarState

/**
 * The app actions as one back stack entry sees them, where only the entry on top of the stack gets
 * to say what the rail and the top bar show.
 *
 * Being composed is not the same as being current. A predictive back gesture composes the entry
 * underneath to preview it, and an entry leaving is composed until its animation ends. Each runs its
 * effects as usual, and if those wrote straight through, a back swipe that was then cancelled would
 * leave the rail and the top bar describing the entry that was only previewed - with nothing to put
 * them back, since the entry still on top never left composition and its effects do not run again.
 *
 * So what is asked for while not current is held rather than dropped, and applied by [applyHeld] if
 * the entry becomes current: a gesture that completes keeps the previewed entry composed, so its
 * effects will not run a second time either.
 */
internal class CurrentEntryAppActions(
    private val app: MawAppActions,
    private val isCurrent: () -> Boolean,
) : MawAppActions by app {
    private var navArea: NavigationArea? = null
    private var topBar: Pair<NavigationArea, TopBarState>? = null
    private var activeYear: Int? = null

    // null is a value worth holding here - it says nobody is current - so it cannot also mean unset
    private var activeFeedSubject: MediaFeedSubject? = null
    private var hasActiveFeedSubject = false

    override fun setNavArea(area: NavigationArea) {
        navArea = area

        if (isCurrent()) {
            app.setNavArea(area)
        }
    }

    override fun updateTopBar(
        area: NavigationArea,
        state: TopBarState,
    ) {
        topBar = area to state

        if (isCurrent()) {
            app.updateTopBar(area, state)
        }
    }

    override fun setActiveYear(year: Int) {
        activeYear = year

        if (isCurrent()) {
            app.setActiveYear(year)
        }
    }

    override fun setActiveFeedSubject(subject: MediaFeedSubject?) {
        activeFeedSubject = subject
        hasActiveFeedSubject = true

        if (isCurrent()) {
            app.setActiveFeedSubject(subject)
        }
    }

    // the area goes first: the top bar turns away an update from any area but the current one
    fun applyHeld() {
        navArea?.let { app.setNavArea(it) }
        activeYear?.let { app.setActiveYear(it) }

        if (hasActiveFeedSubject) {
            app.setActiveFeedSubject(activeFeedSubject)
        }

        topBar?.let { (area, state) -> app.updateTopBar(area, state) }
    }
}

@Composable
internal fun ProvideCurrentEntryAppActions(
    key: NavKey,
    navigationState: NavigationState,
    content: @Composable () -> Unit,
) {
    val app = LocalMawAppActions.current
    val actions = remember(app, key, navigationState) {
        CurrentEntryAppActions(app) { navigationState.currentKey == key }
    }
    val isCurrent by remember(key, navigationState) {
        derivedStateOf { navigationState.currentKey == key }
    }

    LaunchedEffect(actions, isCurrent) {
        if (isCurrent) {
            actions.applyHeld()
        }
    }

    CompositionLocalProvider(LocalMawAppActions provides actions, content = content)
}
