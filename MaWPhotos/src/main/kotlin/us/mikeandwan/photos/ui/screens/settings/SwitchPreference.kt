package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SwitchPreference(
    labelStringId: Int,
    isChecked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp, 16.dp, 8.dp),
    ) {
        Text(
            style = MaterialTheme.typography.titleSmall,
            text = stringResource(id = labelStringId),
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { onChange(it) },
        )
    }
}
