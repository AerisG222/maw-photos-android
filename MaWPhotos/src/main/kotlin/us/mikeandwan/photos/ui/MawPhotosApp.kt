package us.mikeandwan.photos.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.components.navigation.NavigationRail
import us.mikeandwan.photos.ui.components.topbar.TopBar
import us.mikeandwan.photos.ui.components.topbar.TopBarState
import us.mikeandwan.photos.ui.navigation.Navigator
import us.mikeandwan.photos.ui.navigation.rememberNavigationState
import us.mikeandwan.photos.ui.navigation.toEntries
import us.mikeandwan.photos.ui.screens.about.AboutRoute
import us.mikeandwan.photos.ui.screens.about.about
import us.mikeandwan.photos.ui.screens.categories.CategoriesRoute
import us.mikeandwan.photos.ui.screens.categories.categories
import us.mikeandwan.photos.ui.screens.category.CategoryRoute
import us.mikeandwan.photos.ui.screens.category.category
import us.mikeandwan.photos.ui.screens.categoryItem.CategoryItemRoute
import us.mikeandwan.photos.ui.screens.categoryItem.categoryItem
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserRoute
import us.mikeandwan.photos.ui.screens.inactiveUser.inactiveUser
import us.mikeandwan.photos.ui.screens.login.LoginRoute
import us.mikeandwan.photos.ui.screens.login.login
import us.mikeandwan.photos.ui.screens.random.RandomRoute
import us.mikeandwan.photos.ui.screens.random.random
import us.mikeandwan.photos.ui.screens.randomItem.RandomItemRoute
import us.mikeandwan.photos.ui.screens.randomItem.randomItem
import us.mikeandwan.photos.ui.screens.search.SearchRoute
import us.mikeandwan.photos.ui.screens.search.search
import us.mikeandwan.photos.ui.screens.settings.SettingsRoute
import us.mikeandwan.photos.ui.screens.settings.settings
import us.mikeandwan.photos.ui.screens.upload.UploadRoute
import us.mikeandwan.photos.ui.screens.upload.upload
import us.mikeandwan.photos.ui.theme.MawPhotosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MawPhotosApp(vm: MawPhotosAppViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val topLevelRoutes = remember {
        setOf(
            CategoriesRoute(null),
            RandomRoute,
            SearchRoute(),
            SettingsRoute,
            UploadRoute,
            AboutRoute,
            LoginRoute,
            InactiveUserRoute,
        )
    }

    val navigationState = rememberNavigationState(
        startRoute = CategoriesRoute(null),
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

    val appActions = remember(vm) {
        object : MawAppActions {
            override fun updateTopBar(state: TopBarState) = vm.updateTopBar(state)
            override fun setNavArea(area: NavigationArea) = vm.setNavArea(area)
        }
    }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        vm.handleIntent(activity?.intent)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val activity = context as? Activity
        vm.handleIntent(activity?.intent)
    }

    LaunchedEffect(Unit) {
        vm.drawerState.collect {
            coroutineScope.launch {
                when (it) {
                    DrawerValue.Closed -> drawerState.close()
                    DrawerValue.Open -> drawerState.open()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.signalNavigate.collect { route ->
            if (route != null) {
                navigator.navigate(route)
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.errorsToDisplay.collect {
            snackbarHostState.showSnackbar(it.message)
        }
    }

    // monitor login status and once logged in, check for active or inactive user
    LaunchedEffect(Unit) {
        vm.authenticationStatus
            .onEach {
                if (it is AuthStatus.Authorized) {
                    vm.queryUserStatus()
                }
            }.collect { }
    }

    val entryProvider = entryProvider {
        login(
            navigateAfterLogin = {
                // this is now handled by monitoring user status
            },
        )
        inactiveUser(
            navigateToLogin = { vm.navigate(LoginRoute) },
            navigateAfterActivated = { vm.navigate(CategoriesRoute(null)) },
        )
        about()
        categories(
            setActiveYear = vm::setActiveYear,
            navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
            navigateToLogin = { vm.navigate(LoginRoute) },
            navigateToCategories = { vm.navigate(CategoriesRoute(it)) },
        )
        category(
            navigateToMedia = {
                vm.navigate(
                    CategoryItemRoute(it.categoryId, it.id),
                )
            },
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        categoryItem(
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        random(
            navigateToMedia = { vm.navigate(RandomItemRoute(it)) },
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        randomItem(
            navigateToYear = { vm.navigate(CategoriesRoute(it)) },
            navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        search(
            navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        settings(
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
        upload(
            navigateToLogin = { vm.navigate(LoginRoute) },
        )
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
                            navigateToCategories = {
                                vm.navigateAndCloseDrawer(
                                    CategoriesRoute(null),
                                )
                            },
                            navigateToCategoriesByYear = {
                                vm.navigateAndCloseDrawer(
                                    CategoriesRoute(it),
                                )
                            },
                            navigateToRandom = { vm.navigateAndCloseDrawer(RandomRoute) },
                            navigateToSearch = { vm.navigateAndCloseDrawer(SearchRoute()) },
                            navigateToSearchWithTerm = {
                                vm.navigateAndCloseDrawer(
                                    SearchRoute(it),
                                )
                            },
                            navigateToSettings = { vm.navigateAndCloseDrawer(SettingsRoute) },
                            navigateToUpload = { vm.navigateAndCloseDrawer(UploadRoute) },
                            navigateToAbout = { vm.navigateAndCloseDrawer(AboutRoute) },
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
                                onExpandNavMenu = { vm.openDrawer() },
                                onBackClicked = {
                                    val currentStack =
                                        navigationState.backStacks[navigationState.topLevelRoute]
                                    if (currentStack != null && currentStack.size > 1) {
                                        navigator.goBack()
                                    } else {
                                        navigator.navigate(CategoriesRoute(null))
                                    }
                                },
                                onSearch = { vm.navigate(SearchRoute(it)) },
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
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    ) {
                        NavDisplay(
                            entries = navigationState.toEntries(entryProvider),
                            onBack = { navigator.goBack() },
                            sceneStrategy = remember { DialogSceneStrategy() },
                        )
                    }
                }
            }
        }
    }
}
