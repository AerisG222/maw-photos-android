package us.mikeandwan.photos.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.domain.CategoryPreferenceRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.ExternalCallStatus

data class CategoriesUiState(
    val year: Int? = null,
    val categories: List<Category> = emptyList(),
    val isRefreshing: Boolean = false,
    val preferences: CategoryPreference = CategoryPreference(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val invalidYearMostRecent: Int? = null,
)

@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        categoryPreferenceRepository: CategoryPreferenceRepository,
        private val errorRepository: ErrorRepository,
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

            combine(
                categoryRepository.getYears(),
                categories,
                _year,
                _isRefreshing,
                categoryPreferenceRepository.getCategoryPreference(),
            ) { years, categories, year, isRefreshing, preferences ->
                var isLoading = true
                var invalidYearMostRecent: Int? = null

                if (year == null) {
                    if (years.isNotEmpty()) setYear(years.max())
                } else {
                    isLoading = categories.isEmpty()
                    if (years.isNotEmpty() && !years.contains(year)) {
                        invalidYearMostRecent = years.max()
                    }
                }

                CategoriesUiState(
                    year = year,
                    categories = categories,
                    isRefreshing = isRefreshing,
                    preferences = preferences,
                    isLoading = isLoading,
                    invalidYearMostRecent = invalidYearMostRecent,
                )
            }.onEach { state ->
                _uiState.update { state }
            }.launchIn(viewModelScope)
        }

        fun setYear(year: Int?) {
            _year.update { year }
        }

        fun refreshCategories() {
            viewModelScope.launch {
                categoryRepository
                    .getUpdatedCategories()
                    .onEach {
                        when (it) {
                            is ExternalCallStatus.Loading -> {
                                delay(10.milliseconds)
                                _isRefreshing.update { true }
                            }

                            is ExternalCallStatus.Success -> {
                                delay(10.milliseconds)
                                _isRefreshing.update { false }

                                val msg = when (it.result.count()) {
                                    0 -> "No updates available"
                                    1 -> "1 category updated"
                                    else -> "${it.result.count()} categories updated"
                                }

                                errorRepository.showThenClearError(msg)
                            }

                            is ExternalCallStatus.Error -> {
                                delay(10.milliseconds)
                                _isRefreshing.update { false }

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
