package us.mikeandwan.photos.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.mikeandwan.photos.BuildConfig
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.Category
import kotlin.uuid.Uuid

abstract class BaseCategoryViewModel (
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _category = MutableStateFlow<Category?>(null)
    val category = _category.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media = _media.asStateFlow()

    fun loadCategory(categoryId: Uuid) {
        if(category.value?.id == categoryId) {
            return
        }

        _category.value = null
        _media.value = emptyList()

        viewModelScope.launch {
            if(BuildConfig.DEBUG) {
                delay(500)
            }

            categoryRepository
                .getCategory(categoryId)
                .collect { _category.value = it }
        }
    }

    fun loadMedia(categoryId: Uuid) {
        if(
            category.value?.id == categoryId &&
            media.value.isNotEmpty()
        ) {
            return
        }

        viewModelScope.launch {
            if(BuildConfig.DEBUG) {
                delay(1000)
            }

            categoryRepository
                .getMedia(categoryId)
                .filter { it is ExternalCallStatus.Success }
                .map { it as ExternalCallStatus.Success }
                .map { it.result }
                .collect { _media.value = it }
        }
    }
}
