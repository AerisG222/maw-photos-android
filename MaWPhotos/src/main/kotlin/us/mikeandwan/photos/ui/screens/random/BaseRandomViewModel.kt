package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.RandomMediaRepository

abstract class BaseRandomViewModel(
    protected val randomMediaRepository: RandomMediaRepository,
) : ViewModel() {
    // todo: allow videos in random area?
    val media = randomMediaRepository.media

    fun loadNextPage() {
        viewModelScope.launch {
            randomMediaRepository
                .loadNextPage()
                .collect { }
        }
    }
}
