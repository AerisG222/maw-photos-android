package us.mikeandwan.photos.ui.components.topbar

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

data class TopBarState(
    var show: Boolean = true,
    var showAppIcon: Boolean = true,
    var title: String = "",
    var initialSearchTerm: String = "",
    var showSearch: Boolean = false,
    var tinyVerticalTitlePrefix: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    state: TopBarState,
    onExpandNavMenu: () -> Unit,
    onBackClicked: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            if (state.showSearch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(0.dp),
                ) {
                    TopSearchBar(
                        initialSearchTerm = state.initialSearchTerm,
                        onSearch = onSearch,
                    )
                }
            } else {
                Column {
                    if (state.tinyVerticalTitlePrefix.isNotEmpty()) {
                        Text(
                            text = state.tinyVerticalTitlePrefix,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }

                    Text(
                        text = state.title,
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                    )
                }
            }
        },
        navigationIcon = {
            if (state.showAppIcon) {
                IconButton(onClick = onExpandNavMenu) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launch),
                        contentDescription = stringResource(R.string.application_menu_icon_description),
                        tint = Color.Unspecified,
                    )
                }
            } else {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.navigate_back_icon_description),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TopBarPreview() {
    MaterialTheme {
        TopBar(
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            state = TopBarState(
                showAppIcon = true,
                title = "Test Title",
                tinyVerticalTitlePrefix = "2024",
            ),
            onExpandNavMenu = {},
            onBackClicked = {},
            onSearch = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TopBarBackPreview() {
    MaterialTheme {
        TopBar(
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            state = TopBarState(
                showAppIcon = false,
                title = "Back Title",
            ),
            onExpandNavMenu = {},
            onBackClicked = {},
            onSearch = {},
        )
    }
}
