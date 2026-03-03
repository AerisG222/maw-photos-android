package us.mikeandwan.photos.ui

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.components.navigation.NavigationRail
import us.mikeandwan.photos.ui.components.topbar.TopBar
import us.mikeandwan.photos.ui.navigation.Navigator
import us.mikeandwan.photos.ui.navigation.rememberNavigationState
import us.mikeandwan.photos.ui.navigation.toEntries
import us.mikeandwan.photos.ui.screens.about.AboutNavKey
import us.mikeandwan.photos.ui.screens.about.about
import us.mikeandwan.photos.ui.screens.categories.CategoriesNavKey
import us.mikeandwan.photos.ui.screens.categories.categories
import us.mikeandwan.photos.ui.screens.category.category
import us.mikeandwan.photos.ui.screens.categoryItem.categoryItem
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserNavKey
import us.mikeandwan.photos.ui.screens.inactiveUser.inactiveUser
import us.mikeandwan.photos.ui.screens.login.LoginNavKey
import us.mikeandwan.photos.ui.screens.login.login
import us.mikeandwan.photos.ui.screens.random.RandomNavKey
import us.mikeandwan.photos.ui.screens.random.random
import us.mikeandwan.photos.ui.screens.randomItem.randomItem
import us.mikeandwan.photos.ui.screens.search.SearchNavKey
import us.mikeandwan.photos.ui.screens.search.search
import us.mikeandwan.photos.ui.screens.settings.SettingsNavKey
import us.mikeandwan.photos.ui.screens.settings.settings
import us.mikeandwan.photos.ui.screens.upload.UploadNavKey
import us.mikeandwan.photos.ui.screens.upload.upload
import us.mikeandwan.photos.ui.theme.MawPhotosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MawPhotosApp(vm: MawPhotosAppViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val topLevelRoutes = remember {
        setOf(
            CategoriesNavKey(null),
            RandomNavKey,
            SearchNavKey(),
            SettingsNavKey,
            UploadNavKey,
            AboutNavKey,
            LoginNavKey,
            InactiveUserNavKey,
        )
    }

    val navigationState = rememberNavigationState(
        startRoute = CategoriesNavKey(null),
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val years by vm.years.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentSearchTerms by vm.recentSearchTerms.collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )
    val navArea by vm.navArea.collectAsStateWithLifecycle()
    val topBarState by vm.topBarState.collectAsStateWithLifecycle()
    val enableDrawerGestures by vm.enableDrawerGestures.collectAsStateWithLifecycle()
    val activeYear by vm.activeYear.collectAsStateWithLifecycle()

    val appActions = rememberMawAppActions(
        vm = vm,
        navigator = navigator,
        drawerState = drawerState,
        navigationState = navigationState,
        coroutineScope = coroutineScope,
    )

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        vm.handleIntent(activity?.intent)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val activity = context as? Activity
        vm.handleIntent(activity?.intent)
    }

    LaunchedEffect(Unit) {
        launch {
            vm.drawerState.collect {
                when (it) {
                    DrawerValue.Closed -> appActions.closeDrawer()
                    DrawerValue.Open -> appActions.openDrawer()
                }
            }
        }

        launch {
            vm.navigationEvents.collect { route ->
                appActions.navigate(route)
            }
        }

        launch {
            vm.errorsToDisplay.collect {
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }

    val entryProvider = entryProvider {
        login()
        inactiveUser()
        about()
        categories()
        category()
        categoryItem()
        random()
        randomItem()
        search()
        settings()
        upload()
    }

    MawPhotosTheme {
        CompositionLocalProvider(LocalMawAppActions provides appActions) {
            ModalNavigationDrawer(
                gesturesEnabled = enableDrawerGestures,
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        NavigationRail(
                            activeArea = navArea,
                            years = years,
                            activeYear = activeYear,
                            recentSearchTerms = recentSearchTerms,
                            fetchRandomPhotos = vm::fetchRandomPhotos,
                            clearRandomPhotos = vm::clearRandomPhotos,
                            clearSearchHistory = vm::clearSearchHistory,
                            navigateToCategories = { appActions.navigateToCategories(null) },
                            navigateToCategoriesByYear = { appActions.navigateToCategories(it) },
                            navigateToRandom = { appActions.navigateToRandom() },
                            navigateToSearch = { appActions.navigateToSearch() },
                            navigateToSearchWithTerm = { appActions.navigateToSearch(it) },
                            navigateToSettings = { appActions.navigateToSettings() },
                            navigateToUpload = { appActions.navigateToUpload() },
                            navigateToAbout = { appActions.navigateToAbout() },
                        )
                    }
                },
            ) {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        if (topBarState.show) {
                            TopBar(
                                scrollBehavior,
                                state = topBarState,
                                onExpandNavMenu = { appActions.openDrawer() },
                                onBackClicked = { navigator.goBack() },
                                onSearch = { appActions.navigateToSearch(it) },
                            )
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                snackbarData = data,
                            )
                        }
                    },
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        entries = navigationState.toEntries(entryProvider),
                        onBack = { appActions.back() },
                        sceneStrategy = remember { DialogSceneStrategy() },
                    )
                }
            }
        }
    }
}
