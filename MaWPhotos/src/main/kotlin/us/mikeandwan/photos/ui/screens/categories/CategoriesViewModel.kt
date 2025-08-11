package us.mikeandwan.photos.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.CategoriesLoadedGuard
import us.mikeandwan.photos.domain.guards.GuardStatus
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Category
import javax.inject.Inject
import com.hoc081098.flowext.combine
import kotlinx.coroutines.delay
import us.mikeandwan.photos.domain.ConfigRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.models.CategoryPreference

sealed class CategoriesState {
    data object Unknown : CategoriesState()
    data object NotAuthorized : CategoriesState()
    data class InvalidYear(val mostRecentYear: Int) : CategoriesState()
    data object Error : CategoriesState()
    data class Valid(
        val year: Int,
        val categories: List<Category>,
        val isRefreshing: Boolean,
        val preferences: CategoryPreference,
        val refreshCategories: () -> Unit,
        val clearRefreshStatus: () -> Unit
    ) : CategoriesState()
}

@HiltViewModel
class CategoriesViewModel @Inject constructor (
    private val categoryRepository: CategoryRepository,
    authGuard: AuthGuard,
    categoriesLoadedGuard: CategoriesLoadedGuard,
    categoryPreferenceRepository: CategoryPreferenceRepository,
    private val errorRepository: ErrorRepository,
    private val configRepository: ConfigRepository
): ViewModel() {
    private val _year = MutableStateFlow<Int?>(null)
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _preferences = categoryPreferenceRepository
        .getCategoryPreference()
        .stateIn(viewModelScope, WhileSubscribed(5000), CategoryPreference())

    init {
        viewModelScope.launch {
            categoryRepository
                .getYears()
                .onEach { years ->
                    if (_year.value == null && !years.isEmpty()) {
                        setYear(years.max())
                    }
                }
                .collect { }
        }

        viewModelScope.launch {
            configRepository
                .loadScales()
                .collect { }
        }

        viewModelScope.launch {
            _year.onEach {
                if (it != null) {
                    categoryRepository
                        .loadCategories(it)
                        .collect { }
                }
            }
            .collect { }
        }
    }

    fun setYear(year: Int?) {
        _year.value = year
    }

    fun clearRefreshStatus() {
        _isRefreshing.value = false
    }

    private var isFetchingCategories = false

    val state = combine(
        authGuard.status,
        categoriesLoadedGuard.status,
        categoryRepository.getYears(),
        _categories,
        _year,
        _isRefreshing,
        _preferences
    ) {
        authStatus,
        categoriesStatus,
        years,
        categories,
        year,
        isRefreshing,
        preferences ->

        when(authStatus) {
            is GuardStatus.NotInitialized -> authGuard.initializeGuard()
            is GuardStatus.Failed -> CategoriesState.NotAuthorized
            is GuardStatus.Passed -> {
                if (year == null)
                    CategoriesState.Unknown
                else
                    when(categoriesStatus) {
                        is GuardStatus.NotInitialized -> categoriesLoadedGuard.initializeGuard()
                        is GuardStatus.Failed -> CategoriesState.Error
                        is GuardStatus.Passed ->
                            when {
                                years.isEmpty() -> CategoriesState.Unknown
                                !years.contains(year) -> CategoriesState.InvalidYear(years.max())
                                categories.isEmpty() -> {
                                    if (!isFetchingCategories) {
                                        isFetchingCategories = true
                                        loadCategories(year)
                                    }
                                    CategoriesState.Unknown
                                }
                                else -> CategoriesState.Valid(
                                    year,
                                    categories,
                                    isRefreshing,
                                    preferences,
                                    refreshCategories = { refreshCategories() },
                                    clearRefreshStatus = {
                                        _isRefreshing.value = false
                                    }
                                )
                            }
                    }
                }
            }
        }
        .stateIn(viewModelScope, WhileSubscribed(5000), CategoriesState.Unknown)

    private fun refreshCategories() {
        viewModelScope.launch {
            categoryRepository
                .getUpdatedCategories()
                .onEach {
                    when(it) {
                        is ExternalCallStatus.Loading -> {
                            delay(10)
                            _isRefreshing.value = true
                        }
                        is ExternalCallStatus.Success -> {
                            delay(10)
                            _isRefreshing.value = false

                            val msg = when(it.result.count()) {
                                0 -> "No new categories available"
                                1 -> "One new category loaded"
                                else -> "${it.result.count()} categories loaded"
                            }

                            errorRepository.showThenClearError(msg)
                        }
                        is ExternalCallStatus.Error -> {
                            delay(10)
                            _isRefreshing.value = false

                            Timber.e(it.message)
                            Timber.e(it.cause)

                            errorRepository.showThenClearError("There was an error loading categories")
                        }
                    }
                }
                .catch { e -> Timber.e(e) }
                .launchIn(this)
        }
    }

    private fun loadCategories(year: Int) {
        viewModelScope.launch {
            categoryRepository
                .getCategories(year)
                .collect { cats -> _categories.value = cats }
        }
    }
}
