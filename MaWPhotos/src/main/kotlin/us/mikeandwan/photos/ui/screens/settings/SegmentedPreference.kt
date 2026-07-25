package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

/**
 * A preference with few enough choices to show them all inline.  Anything with a long list of
 * options (slideshow intervals, search counts) still belongs in [MenuPreference]'s dialog.
 */
@Composable
fun SegmentedPreference(
    labelStringId: Int,
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp, 8.dp, 8.dp),
    ) {
        Text(
            text = stringResource(id = labelStringId),
            style = MaterialTheme.typography.titleSmall,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selectedValue,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                ) {
                    Text(text = option)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedPreferencePreview() {
    SegmentedPreference(
        labelStringId = R.string.grid_thumbnail_size,
        options = listOf("Small", "Medium", "Large"),
        selectedValue = "Medium",
        onSelect = {},
    )
}
