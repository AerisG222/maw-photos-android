package us.mikeandwan.photos.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media

abstract class BaseCategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _category = MutableStateFlow<Category?>(null)
    val category = _category.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media = _media.asStateFlow()

    private var categoryJob: Job? = null
    private var mediaJob: Job? = null

    fun reset() {
        _category.value = null
        _media.value = emptyList()
    }

    fun loadCategory(categoryId: Uuid) {
        if (category.value?.id == categoryId) {
            return
        }

        _category.value = null
        _media.value = emptyList()

        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                delay(500)
            }

            categoryRepository
                .getCategory(categoryId)
                .collect { _category.value = it }
        }
    }

    fun loadMedia(categoryId: Uuid) {
        if (category.value?.id == categoryId && media.value.isNotEmpty()) {
            return
        }

        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                delay(1000)
            }

            categoryRepository
                .getMedia(categoryId)
                .filterIsInstance<ExternalCallStatus.Success<List<Media>>>()
                .map { it.result }
                .collect { _media.value = it }
        }
    }
}
