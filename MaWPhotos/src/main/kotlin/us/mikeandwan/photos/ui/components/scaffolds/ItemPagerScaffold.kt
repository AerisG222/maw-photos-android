package us.mikeandwan.photos.ui.components.scaffolds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ItemPagerScaffold(
    modifier: Modifier = Modifier,
    showDetails: Boolean = false,
    topLeftContent: @Composable () -> Unit = {},
    topRightContent: @Composable () -> Unit = {},
    bottomBarContent: @Composable () -> Unit = {},
    detailSheetContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    content()

    val bgColor = MaterialTheme.colorScheme.primaryContainer

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = bgColor),
        ) {
            topLeftContent()
            Spacer(Modifier.weight(1f))
            topRightContent()
        }

        Row(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
                .background(color = bgColor),
        ) {
            bottomBarContent()
        }
    }

    if (showDetails) {
        detailSheetContent()
    }
}
