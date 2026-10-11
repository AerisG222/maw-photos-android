package us.mikeandwan.photos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    // keyed by the route rather than left to fall where it lands in the loop: remember is
    // positional, so without this every stack is identified by its position among the others and
    // inserting a top level route would hand an area the stack belonging to its neighbour
    val backStacks = topLevelRoutes.associateWith { route ->
        key(route) { rememberNavBackStack(route) }
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    // Every stack is decorated, not just the ones on screen.  That is what lets an area the user
    // has navigated away from keep what it had: its entries stay listed by its own
    // rememberDecoratedNavEntries, so leaving composition is not mistaken for being popped, and
    // only a real pop clears the state belonging to an entry.
    val decoratedEntries = backStacks.mapValues { (route, stack) ->
        key(route) {
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                // Scopes each entry's view models to that entry rather than to the activity, so a
                // level of the tree keeps its own state while it is on the stack and gives it up
                // when it is popped - which is what the routes already assume, every one of them
                // reading what it is showing out of its key.  It has to sit inside the saveable
                // decorator above, which is what a SavedStateHandle reaches through, so the order
                // is load bearing.
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }
    }

    // Use a stable remembered list so NavDisplay doesn't see a new object on every recomposition.
    // A new object each time would cause NavDisplay to recompose its entire content on any state
    // change in MawPhotosApp, creating a recomposition cascade that starves the event queue.
    val result = remember { mutableStateListOf<NavEntry<NavKey>>() }
    val newEntries = stacksInUse.flatMap { decoratedEntries[it] ?: emptyList() }

    if (result.size != newEntries.size || result.indices.any { result[it] !== newEntries[it] }) {
        result.clear()
        result.addAll(newEntries)
    }

    return result
}
