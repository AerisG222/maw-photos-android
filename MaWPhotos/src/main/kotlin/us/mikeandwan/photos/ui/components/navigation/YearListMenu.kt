package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun YearListMenu(
    years: List<Int>,
    activeYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerModifier = modifier.then(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.secondaryContainer),
    )

    val yearDividerModifier = Modifier
        .padding(16.dp, 0.dp)
        .background(color = MaterialTheme.colorScheme.secondary)

    LazyColumn(containerModifier) {
        itemsIndexed(
            years,
            key = { index, item -> item },
        ) { index, year ->
            YearListItem(
                year,
                year == activeYear,
                onYearSelected,
            )

            if (index != years.size - 1) {
                HorizontalDivider(
                    modifier = yearDividerModifier,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}
