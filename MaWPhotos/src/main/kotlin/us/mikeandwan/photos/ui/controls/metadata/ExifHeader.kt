package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExifHeader(
    name: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = name,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(4.dp, 2.dp)
        )
    }
}
