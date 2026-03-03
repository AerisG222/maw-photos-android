package us.mikeandwan.photos.ui

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.components.topbar.TopBarState
import us.mikeandwan.photos.ui.navigation.NavigationState
import us.mikeandwan.photos.ui.navigation.Navigator
import us.mikeandwan.photos.ui.screens.about.AboutNavKey
import us.mikeandwan.photos.ui.screens.categories.CategoriesNavKey
import us.mikeandwan.photos.ui.screens.category.CategoryNavKey
import us.mikeandwan.photos.ui.screens.categoryItem.CategoryItemNavKey
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserNavKey
import us.mikeandwan.photos.ui.screens.login.LoginNavKey
import us.mikeandwan.photos.ui.screens.random.RandomNavKey
import us.mikeandwan.photos.ui.screens.randomItem.RandomItemNavKey
import us.mikeandwan.photos.ui.screens.search.SearchNavKey
import us.mikeandwan.photos.ui.screens.settings.SettingsNavKey
import us.mikeandwan.photos.ui.screens.upload.UploadNavKey

@Composable
fun rememberMawAppActions(
    vm: MawPhotosAppViewModel,
    navigator: Navigator,
    drawerState: DrawerState,
    navigationState: NavigationState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MawAppActions =
    remember(vm, navigator, drawerState, navigationState, coroutineScope) {
        MawAppActionsImpl(vm, navigator, drawerState, navigationState, coroutineScope)
    }

private class MawAppActionsImpl(
    private val vm: MawPhotosAppViewModel,
    private val navigator: Navigator,
    private val drawerState: DrawerState,
    private val navigationState: NavigationState,
    private val coroutineScope: CoroutineScope,
) : MawAppActions {
    override fun updateTopBar(
        area: NavigationArea,
        state: TopBarState,
    ) = vm.updateTopBar(area, state)

    override fun setNavArea(area: NavigationArea) = vm.setNavArea(area)

    override fun setActiveYear(year: Int) = vm.setActiveYear(year)

    override fun openDrawer() {
        coroutineScope.launch { drawerState.open() }
    }

    override fun closeDrawer() {
        coroutineScope.launch { drawerState.close() }
    }

    override fun navigate(route: NavKey) {
        closeDrawer()
        navigator.navigate(route)
    }

    override fun navigateToAbout() {
        navigate(AboutNavKey)
    }

    override fun navigateToCategories(year: Int?) {
        navigate(CategoriesNavKey(year))
    }

    override fun navigateToCategory(categoryId: Uuid) {
        navigate(CategoryNavKey(categoryId))
    }

    override fun navigateToCategoryItem(
        categoryId: Uuid,
        mediaId: Uuid,
    ) {
        navigate(CategoryItemNavKey(categoryId, mediaId))
    }

    override fun navigateToInactiveUser() {
        navigate(InactiveUserNavKey)
    }

    override fun navigateToLogin() {
        navigate(LoginNavKey)
    }

    override fun navigateToRandom() {
        navigate(RandomNavKey)
    }

    override fun navigateToRandomItem(mediaId: Uuid) {
        navigate(RandomItemNavKey(mediaId))
    }

    override fun navigateToSearch(searchTerm: String?) {
        navigate(SearchNavKey(searchTerm))
    }

    override fun navigateToSettings() {
        navigate(SettingsNavKey)
    }

    override fun navigateToUpload() {
        navigate(UploadNavKey)
    }

    override fun back() {
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
        if (currentStack != null && currentStack.size > 1) {
            navigator.goBack()
        } else {
            navigateToCategories(null)
        }
    }
}
