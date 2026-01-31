package us.mikeandwan.photos.ui.controls.mediapager

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OverlayPositionCount(
    position: Int,
    count: Int,
) {
    Row(modifier = Modifier.padding(4.dp, 2.dp)) {
        Text(text = position.toString())
        Text(text = " / ")
        Text(text = count.toString())
    }
}
