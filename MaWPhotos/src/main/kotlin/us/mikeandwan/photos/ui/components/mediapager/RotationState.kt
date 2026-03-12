package us.mikeandwan.photos.ui.components.mediapager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.uuid.Uuid

class RotationState(
    val activeRotation: Float,
    val setActiveRotation: (Float) -> Unit,
)

@Composable
fun rememberRotation(activeId: Uuid): RotationState {
    val rotationDictionary = rememberSaveable { HashMap<Uuid, Float>() }
    val (activeRotation, setActiveRotation) = rememberSaveable { mutableFloatStateOf(0f) }

    fun getRotationForIndex(id: Uuid): Float =
        if (id == Uuid.NIL) {
            0f
        } else {
            when (rotationDictionary.containsKey(id)) {
                true -> rotationDictionary[id]!!
                false -> 0f
            }
        }

    fun updateRotation(deg: Float) {
        val currRotation = getRotationForIndex(activeId)
        val newRotation = currRotation + deg

        if (activeId != Uuid.NIL) {
            rotationDictionary[activeId] = newRotation
            setActiveRotation(newRotation)
        }
    }

    LaunchedEffect(activeId) {
        setActiveRotation(getRotationForIndex(activeId))
    }

    return RotationState(
        activeRotation,
        setActiveRotation = ::updateRotation,
    )
}
