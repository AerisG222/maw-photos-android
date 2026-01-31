package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MenuPreference(
    labelStringId: Int,
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
) {
    val (display, setDisplay) = remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { setDisplay(!display) },
    ) {
        MenuPreferenceSummary(
            labelStringId = labelStringId,
            selectedValue = selectedValue,
        )

        if (display) {
            Dialog(onDismissRequest = { setDisplay(false) }) {
                MenuPreferenceCard(
                    labelStringId = labelStringId,
                    options = options,
                    selectedValue = selectedValue,
                    onSelect = {
                        setDisplay(false)
                        onSelect(it)
                    },
                    onCancel = { setDisplay(false) },
                )
            }
        }
    }
}
