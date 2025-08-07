package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ExifDetail(
    bgColor: androidx.compose.ui.graphics.Color,
    txtColor: androidx.compose.ui.graphics.Color,
    detail: JsonObject
) {
    Row(Modifier
        .fillMaxWidth()
        .background(bgColor)
    ) {
        Text(
            text = detail["desc"]?.jsonPrimitive?.content ?: "",
            color = txtColor,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp, 2.dp)
        )
        Text(
            text = detail["val"]?.jsonPrimitive?.content ?: "",
            color = txtColor,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp, 2.dp)
        )
    }
}
