package us.mikeandwan.photos.ui.controls.topbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R

data class TopBarState(
    var show: Boolean = true,
    var showAppIcon: Boolean = true,
    var title: String = "",
    var initialSearchTerm: String = "",
    var showSearch: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    state: TopBarState,
    onExpandNavMenu: () -> Unit,
    onBackClicked: () -> Unit,
    onSearch: (String) -> Unit
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            if(state.showSearch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(0.dp)
                ) {
                    TopSearchBar(
                        state.initialSearchTerm,
                        onSearch
                    )
                }
            } else {
                Text(
                    text = state.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if(state.showAppIcon) {
                AsyncImage(
                    model = R.drawable.ic_launch,
                    contentDescription = stringResource(R.string.application_menu_icon_description),
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp)
                        .padding(8.dp)
                        .clickable { onExpandNavMenu() }
                )
            } else {
                AsyncImage(
                    model = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.navigate_back_icon_description),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp)
                        .padding(8.dp)
                        .clickable { onBackClicked() }
                )
            }
        }
    )
}
