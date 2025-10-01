package us.mikeandwan.photos.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.authorization.AuthStatus
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.controls.navigation.NavigationRail
import us.mikeandwan.photos.ui.controls.topbar.TopBar
import us.mikeandwan.photos.ui.screens.about.AboutRoute
import us.mikeandwan.photos.ui.screens.about.aboutScreen
import us.mikeandwan.photos.ui.screens.categories.CategoriesRoute
import us.mikeandwan.photos.ui.screens.categories.categoriesScreen
import us.mikeandwan.photos.ui.screens.category.CategoryRoute
import us.mikeandwan.photos.ui.screens.category.categoryScreen
import us.mikeandwan.photos.ui.screens.categoryItem.CategoryItemRoute
import us.mikeandwan.photos.ui.screens.categoryItem.categoryItemScreen
import us.mikeandwan.photos.ui.screens.inactiveUser.InactiveUserRoute
import us.mikeandwan.photos.ui.screens.inactiveUser.inactiveUserScreen
import us.mikeandwan.photos.ui.screens.login.LoginRoute
import us.mikeandwan.photos.ui.screens.login.loginScreen
import us.mikeandwan.photos.ui.screens.random.RandomRoute
import us.mikeandwan.photos.ui.screens.random.randomScreen
import us.mikeandwan.photos.ui.screens.randomItem.RandomItemRoute
import us.mikeandwan.photos.ui.screens.randomItem.randomItemScreen
import us.mikeandwan.photos.ui.screens.search.SearchRoute
import us.mikeandwan.photos.ui.screens.search.searchScreen
import us.mikeandwan.photos.ui.screens.settings.SettingsRoute
import us.mikeandwan.photos.ui.screens.settings.settingsScreen
import us.mikeandwan.photos.ui.screens.upload.UploadRoute
import us.mikeandwan.photos.ui.screens.upload.uploadScreen
import us.mikeandwan.photos.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val activity = this

        setContent {
            val vm = hiltViewModel<MainViewModel>()

            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            val snackbarHostState = remember { SnackbarHostState() }
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            val years by vm.years.collectAsStateWithLifecycle(initialValue = emptyList())
            val recentSearchTerms by vm.recentSearchTerms.collectAsStateWithLifecycle(initialValue = emptyList())
            val navArea by vm.navArea.collectAsStateWithLifecycle()
            val topBarState by vm.topBarState.collectAsStateWithLifecycle()
            val enableDrawerGestures by vm.enableDrawerGestures.collectAsStateWithLifecycle()
            val activeYear by vm.activeYear.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                handleIntent(activity.intent ?: Intent(), vm, navController)
            }

            LaunchedEffect(Unit) {
                vm.drawerState.collect {
                    coroutineScope.launch {
                        when(it) {
                            DrawerValue.Closed -> drawerState.close()
                            DrawerValue.Open -> drawerState.open()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                vm.signalNavigate.collect { route ->
                    if (route != null) {
                        navController.navigate(route)
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
                vm.authenticationStatus.onEach {
                    if(it is AuthStatus.Authorized) {
                        vm.queryUserStatus()
                    }
                }.collect { }
            }

            // monitor user status and navigate to inactive screen if needed
            LaunchedEffect(Unit) {
                combine(
                    vm.userStatus,
                    vm.authenticationStatus
                ) {
                    userStatus,
                    authStatus ->

                    if(userStatus is UserStatus.Unknown && authStatus is AuthStatus.Authorized) {
                        vm.queryUserStatus()
                    }

                    if(userStatus is UserStatus.Inactive) {
                        Timber.w("YO INACTIVE!")
                        vm.navigate(InactiveUserRoute)
                    }

                    if(userStatus is UserStatus.Active && authStatus is AuthStatus.Authorized) {
                        vm.navigate(CategoriesRoute(null))
                    }
                }.collect {  }
            }

            AppTheme {
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
                                navigateToCategories = { vm.navigateAndCloseDrawer(CategoriesRoute(null)) },
                                navigateToCategoriesByYear = { vm.navigateAndCloseDrawer(CategoriesRoute(it)) },
                                navigateToRandom = { vm.navigateAndCloseDrawer(RandomRoute) },
                                navigateToSearch = { vm.navigateAndCloseDrawer(SearchRoute()) },
                                navigateToSearchWithTerm = { vm.navigateAndCloseDrawer(SearchRoute(it)) },
                                navigateToSettings = { vm.navigateAndCloseDrawer(SettingsRoute) },
                                navigateToUpload = { vm.navigateAndCloseDrawer(UploadRoute) },
                                navigateToAbout = { vm.navigateAndCloseDrawer(AboutRoute) }
                            )
                        }
                    }
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
                                        if(navController.visibleEntries.value.size > 1) {
                                            navController.navigateUp()
                                        } else {
                                            navController.navigate(CategoriesRoute(null))
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
                                    snackbarData = data
                                )
                            }
                        },
                    ) { innerPadding ->
                        // https://issuetracker.google.com/issues/297824100
                        Column(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                modifier = Modifier.padding(innerPadding),
                                navController = navController,
                                startDestination = CategoriesRoute(null)
                            ) {
                                loginScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateAfterLogin = { /* this is now handled by monitoring user status */ }
                                )
                                inactiveUserScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                    navigateAfterActivated = { vm.navigate(CategoriesRoute(null)) }
                                )
                                aboutScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea
                                )
                                categoriesScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    setActiveYear = vm::setActiveYear,
                                    navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                    navigateToCategories = { vm.navigate(CategoriesRoute(it)) }
                                )
                                categoryScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToMedia = { vm.navigate(CategoryItemRoute(it.categoryId, it.id)) },
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                categoryItemScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                randomScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToMedia = { vm.navigate(RandomItemRoute(it)) },
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                randomItemScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToYear = { vm.navigate(CategoriesRoute(it)) },
                                    navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                searchScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToCategory = { vm.navigate(CategoryRoute(it.id)) },
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                settingsScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                                uploadScreen(
                                    updateTopBar = vm::updateTopBar,
                                    setNavArea = vm::setNavArea,
                                    navigateToLogin = { vm.navigate(LoginRoute) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent, vm: MainViewModel, navController: NavController) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                vm.handleSendSingle(intent)
                navController.navigate(UploadRoute)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                vm.handleSendMultiple(intent)
                navController.navigate(UploadRoute)
            }
        }
    }
}
