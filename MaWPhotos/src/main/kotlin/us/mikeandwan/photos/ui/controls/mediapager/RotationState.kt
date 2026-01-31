package us.mikeandwan.photos.ui.controls.mediapager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable

class RotationState(
    val activeRotation: Float,
    val setActiveRotation: (Float) -> Unit,
)

@Composable
fun rememberRotation(activeIndex: Int): RotationState {
    val rotationDictionary = rememberSaveable { HashMap<Int, Float>() }
    val (activeRotation, setActiveRotation) = rememberSaveable { mutableFloatStateOf(0f) }

    fun getRotationForIndex(index: Int): Float =
        when (rotationDictionary.containsKey(index)) {
            true -> rotationDictionary[index]!!
            false -> 0f
        }

    fun updateRotation(deg: Float) {
        val currRotation = getRotationForIndex(activeIndex)
        val newRotation = currRotation + deg

        rotationDictionary[activeIndex] = newRotation
        setActiveRotation(newRotation)
    }

    LaunchedEffect(activeIndex) {
        setActiveRotation(getRotationForIndex(activeIndex))
    }

    return RotationState(
        activeRotation,
        setActiveRotation = ::updateRotation,
    )
}
