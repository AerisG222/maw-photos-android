package us.mikeandwan.photos.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the target package.
 *
 * The app requires an authenticated Auth0 session, and that login is an interactive browser flow
 * that cannot be automated here. The workflow is therefore "log in once on the emulator, then
 * generate": establish a session manually (see the project docs / build setup that re-signs the
 * nonMinifiedRelease build with the debug key so the session survives generation), then run:
 * ```
 * ./gradlew :MaWPhotos:generateDevelopmentReleaseBaselineProfile
 * ```
 *
 * This generator is defensive about auth state: if the app is logged out (login screen shown), it
 * captures a startup-only profile instead of failing, since it cannot drive the browser login.
 * When logged in, it walks the core browse journey (categories -> a category's media -> a single
 * media item) and then the people journey (rail -> people grid -> a person's media -> one item),
 * using stable test tags rather than blind screen coordinates. Every step is a null-safe find, so
 * an account with nothing to show simply contributes less to the profile instead of failing.
 *
 * The tag strings below mirror the app's `testTag` constants (MEDIA_GRID_TAG, MEDIA_GRID_ITEM_TAG,
 * LOGIN_SCREEN_TAG, PEOPLE_GRID_TAG, PERSON_CARD_TAG, APP_MENU_TAG). They are duplicated as
 * literals because this `com.android.test` module does not have the app module on its compile
 * classpath, and must be kept in sync manually. They are surfaced to UiAutomator via
 * `testTagsAsResourceId`, enabled at the app's Compose root. The one content description is
 * likewise a copy of a string resource: the navigation rail's entries carry no test tags, and do
 * not need them - each entry is itself the clickable node.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are
 * supported. The minimum required version of androidx.benchmark is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // The application id for the running build variant is read from the instrumentation arguments.
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // If logged out, the app routes to the login screen. The Auth0 login is a browser flow
            // we can't complete here, so capture a startup-only profile and stop.
            val loggedOut = device.wait(Until.hasObject(By.res(LOGIN_SCREEN_TAG)), UI_TIMEOUT_MS) == true
            if (loggedOut) {
                return@collect
            }

            // Logged in: exercise the core browse journey using stable selectors.
            // 1. Categories grid: wait for it, then scroll.
            scrollGrid()

            // 2. Open the first category.
            device.wait(Until.findObject(By.res(MEDIA_GRID_ITEM_TAG)), UI_TIMEOUT_MS)?.click()
            device.waitForIdle()

            // 3. The selected category's media grid: scroll it.
            scrollGrid()

            // 4. Open the first media item.
            device.wait(Until.findObject(By.res(MEDIA_GRID_ITEM_TAG)), UI_TIMEOUT_MS)?.click()
            device.waitForIdle()

            // 5. Back to the media grid, then back to the categories list.
            device.pressBack()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()

            // 6. Open the navigation drawer and switch to browsing by person.
            //
            // Found by tag rather than by description: the app bar's description sits on the icon,
            // and the icon is not what takes the click - UiAutomator clicks it, logs "Clicking on
            // non-clickable object", and the drawer stays shut. The tag is on the button itself.
            // The retry covers a back press the media pager consumed itself, which leaves one more
            // screen to unwind than expected.
            val appMenu = device.wait(Until.findObject(By.res(APP_MENU_TAG)), UI_TIMEOUT_MS)
                ?: run {
                    device.pressBack()
                    device.waitForIdle()
                    device.wait(Until.findObject(By.res(APP_MENU_TAG)), UI_TIMEOUT_MS)
                }

            appMenu?.click()
            device.waitForIdle()

            // The drawer slides in, and waitForIdle does not wait for that: Compose animations are
            // not what it idles on. Finding the entry is no proof it can be clicked either - the
            // rail is composed even while the drawer is shut - and a click that lands before the
            // sheet has arrived hits the scrim, which dismisses the drawer instead. So settle
            // first, then click, then confirm the people grid actually came up rather than
            // trusting the tap.
            Thread.sleep(DRAWER_SETTLE_MS)
            device.wait(Until.findObject(By.desc(PEOPLE_NAV_DESCRIPTION)), UI_TIMEOUT_MS)?.click()
            device.waitForIdle()

            if (!device.wait(Until.hasObject(By.res(PEOPLE_GRID_TAG)), UI_TIMEOUT_MS)) {
                device.findObject(By.desc(PEOPLE_NAV_DESCRIPTION))?.click()
                device.waitForIdle()
            }

            // 7. The people grid: scroll it, then open the first person.
            scrollGrid(PEOPLE_GRID_TAG)
            device.wait(Until.findObject(By.res(PERSON_CARD_TAG)), UI_TIMEOUT_MS)?.click()
            device.waitForIdle()

            // 8. That person's media, which is the same grid the rest of the app browses.
            scrollGrid()
            device.wait(Until.findObject(By.res(MEDIA_GRID_ITEM_TAG)), UI_TIMEOUT_MS)?.click()
            device.waitForIdle()

            // 9. Unwind: the media item, then the feed, back to the people grid.
            device.pressBack()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.scrollGrid(tag: String = MEDIA_GRID_TAG) {
        device.wait(Until.findObject(By.res(tag)), UI_TIMEOUT_MS)
        device.findObject(By.res(tag))?.apply {
            // Keep the gesture away from the screen edges (system gesture insets).
            setGestureMargin(device.displayWidth / 5)
            fling(Direction.DOWN)
        }
        device.waitForIdle()
    }

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L

        // Comfortably longer than the drawer's slide-in, which is a few hundred milliseconds.
        const val DRAWER_SETTLE_MS = 1_000L

        // Mirror the app's testTag constants (see class doc); keep in sync manually.
        const val MEDIA_GRID_TAG = "mediaGrid"
        const val MEDIA_GRID_ITEM_TAG = "mediaGridItem"
        const val LOGIN_SCREEN_TAG = "loginScreen"
        const val PEOPLE_GRID_TAG = "peopleGrid"
        const val PERSON_CARD_TAG = "personCard"
        const val APP_MENU_TAG = "appMenu"

        // The rail's entries carry no tags, but each one is itself the clickable node, so the
        // description it announces is enough. Mirrors R.string.people_icon_description.
        const val PEOPLE_NAV_DESCRIPTION = "Browse by Person"
    }
}
