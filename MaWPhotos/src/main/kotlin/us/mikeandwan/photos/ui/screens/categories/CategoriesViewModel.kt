package us.mikeandwan.photos.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.flowext.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.CategoriesLoadedGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.ExternalCallStatus

data class CategoriesUiState(
    val year: Int? = null,
    val categories: List<Category> = emptyList(),
    val isRefreshing: Boolean = false,
    val preferences: CategoryPreference = CategoryPreference(),
    val isLoading: Boolean = true,
    val isAuthorized: Boolean = true,
    val error: String? = null,
    val invalidYearMostRecent: Int? = null,
)

@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val authGuard: AuthGuard,
        private val categoriesLoadedGuard: CategoriesLoadedGuard,
        categoryPreferenceRepository: CategoryPreferenceRepository,
        private val errorRepository: ErrorRepository,
        private val configRepository: ConfigRepository,
    ) : ViewModel() {
        private val _year = MutableStateFlow<Int?>(null)
        private val _isRefreshing = MutableStateFlow(false)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val categories = _year
            .flatMapLatest { year ->
                if (year != null) {
                    categoryRepository.getCategories(year)
                } else {
                    flowOf(emptyList())
                }
            }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

        private val _uiState = MutableStateFlow(CategoriesUiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                categoryRepository
                    .getYears()
                    .onEach { years ->
                        if (_year.value == null && years.isNotEmpty()) {
                            setYear(years.max())
                        }
                    }.collect { }
            }

            viewModelScope.launch {
                configRepository
                    .getScales()
                    .collect { }
            }

            // If the app is newly installed there may be no years/categories in the DB yet.
            // Listen for successful authentication and proactively trigger the repository
            // to load years and the most recent year's categories so the UI doesn't stay
            // stuck on the loading state.
            viewModelScope.launch(Dispatchers.IO) {
                authGuard.status.collect { status ->
                    if (status is GuardStatus.Passed) {
                        try {
                            // Ensure scales are present in the DB before loading categories.
                            val initialScales = configRepository.getScales().first()
                            if (initialScales.isEmpty()) {
                                // The repository's getScales() will trigger a load if the DB is empty,
                                // but it emits an empty list first. Wait briefly for a non-empty
                                // emission so downstream category loading has scale data available.
                                val loaded = withTimeoutOrNull(10_000) {
                                    configRepository.getScales().first { it.isNotEmpty() }
                                }

                                if (loaded == null) {
                                    Timber.w("Scales did not load within timeout; continuing without scales")
                                }
                            }

                            val years = categoryRepository.getYears().first()

                            if (years.isEmpty()) {
                                // Trigger an explicit load of years from the API
                                categoryRepository.loadYears(null).collect { /* intentional no-op */ }
                            }

                            val finalYears = categoryRepository.getYears().first()
                            val targetYear = finalYears.maxOrNull()

                            if (targetYear != null) {
                                // Ensure categories for the selected/most-recent year are loaded.
                                val cats = categoryRepository.getCategories(targetYear).first()
                                if (cats.isEmpty()) {
                                    categoryRepository.loadCategories(targetYear).collect { /* no-op */ }
                                }

                                // If the view model hasn't selected a year yet, pick the most-recent.
                                if (_year.value == null) {
                                    _year.value = targetYear
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error loading scales/years/categories after auth")
                        }
                    }
                }
            }

            combine(
                authGuard.status,
                categoriesLoadedGuard.status,
                categoryRepository.getYears(),
                categories,
                _year,
                _isRefreshing,
                categoryPreferenceRepository.getCategoryPreference(),
            ) { authStatus, categoriesStatus, years, categories, year, isRefreshing, preferences ->
                var isAuthorized = true
                var isLoading = true
                var error: String? = null
                var invalidYearMostRecent: Int? = null

                when (authStatus) {
                    is GuardStatus.NotInitialized -> {
                        authGuard.initializeGuard()
                    }

                    is GuardStatus.Failed -> {
                        isAuthorized = false
                    }

                    is GuardStatus.Passed -> {
                        if (year == null) {
                            if (years.isNotEmpty()) setYear(years.max())
                        } else {
                            when (categoriesStatus) {
                                is GuardStatus.NotInitialized -> {
                                    categoriesLoadedGuard.initializeGuard()
                                }

                                is GuardStatus.Failed -> {
                                    error = "Failed to load categories"
                                }

                                is GuardStatus.Passed -> {
                                    isLoading = false
                                    if (years.isNotEmpty() && !years.contains(year)) {
                                        invalidYearMostRecent = years.max()
                                    }
                                }
                            }
                        }
                    }
                }

                CategoriesUiState(
                    year = year,
                    categories = categories,
                    isRefreshing = isRefreshing,
                    preferences = preferences,
                    isLoading = isLoading,
                    isAuthorized = isAuthorized,
                    error = error,
                    invalidYearMostRecent = invalidYearMostRecent,
                )
            }.onEach { state ->
                _uiState.update { state }
            }.launchIn(viewModelScope)
        }

        fun setYear(year: Int?) {
            _year.value = year
        }

        fun refreshCategories() {
            viewModelScope.launch {
                categoryRepository
                    .getUpdatedCategories()
                    .onEach {
                        when (it) {
                            is ExternalCallStatus.Loading -> {
                                delay(10)
                                _isRefreshing.value = true
                            }

                            is ExternalCallStatus.Success -> {
                                delay(10)
                                _isRefreshing.value = false

                                val msg = when (it.result.count()) {
                                    0 -> "No updates available"
                                    1 -> "1 category updated"
                                    else -> "${it.result.count()} categories updated"
                                }

                                errorRepository.showThenClearError(msg)
                            }

                            is ExternalCallStatus.Error -> {
                                delay(10)
                                _isRefreshing.value = false

                                Timber.e(it.message)
                                Timber.e(it.cause)

                                errorRepository.showThenClearError(
                                    "There was an error loading categories",
                                )
                            }
                        }
                    }.catch { e -> Timber.e(e) }
                    .launchIn(this)
            }
        }
    }
