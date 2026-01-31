package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

@Composable
fun MenuPreferenceCard(
    labelStringId: Int,
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .height(400.dp)
            .width(300.dp),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(id = labelStringId),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                LazyColumn(Modifier.height(280.dp)) {
                    itemsIndexed(options) { index, option ->
                        val bgColor = when (option == selectedValue) {
                            true -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.background
                        }

                        val textColor = when (option == selectedValue) {
                            true -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = bgColor)
                                .padding(8.dp),
                        ) {
                            TextButton(
                                onClick = { onSelect(option) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = option,
                                    color = textColor,
                                )
                            }
                        }

                        if (index != options.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = { onCancel() }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        }
    }
}
