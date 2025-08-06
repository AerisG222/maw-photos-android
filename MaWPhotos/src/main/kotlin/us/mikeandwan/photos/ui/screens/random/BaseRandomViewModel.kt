package us.mikeandwan.photos.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.RandomMediaRepository

abstract class BaseRandomViewModel (
    private val randomMediaRepository: RandomMediaRepository
): ViewModel() {
    // todo: allow videos in random area?
    val media = randomMediaRepository.photos

    fun fetch(count: Int) {
        viewModelScope.launch {
            randomMediaRepository
                .fetch(count)
                .collect { }
        }
    }

    fun onResume() {
        // delay is a bit ugly, but when navigating between random list and item screens,
        // the disposable effect ends up firing after the launched effect on the new screen.
        // this results in the fetching to stop.  therefore, we add a short delay here so that
        // setting dofetch should happen after pause that is registered by the disposable effect.
        // also, when navigating to a different area completely, the disposable effect will
        // still have its intended behavior of stopping the automatic fetching
        //
        // this also exposed an issue where the item view would restart at where the user first
        // came into the item view, despite new items being loaded.  for now, lets just stop
        // fetching once a user is in the item view (and no longer need the delay).
        //viewModelScope.launch {
            //delay(1000)
            randomMediaRepository.setDoFetch(true)
        //}
    }

    fun onPause() {
        randomMediaRepository.setDoFetch(false)
    }
}
