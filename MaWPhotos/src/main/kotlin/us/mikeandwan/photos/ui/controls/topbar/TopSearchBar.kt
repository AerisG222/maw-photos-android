package us.mikeandwan.photos.ui.controls.topbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    initialSearchTerm: String,
    onSearch: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val (searchTerm, setSearchTerm) = remember{ mutableStateOf(initialSearchTerm) }

    LaunchedEffect(initialSearchTerm) {
        setSearchTerm(initialSearchTerm)
    }

    fun search(term: String) {
        keyboardController?.hide()
        onSearch(term)
    }

    SearchBar(
        expanded = false,
        onExpandedChange = { },
        inputField = {
            SearchInputField(
                query = searchTerm,
                onSearch = { search(it) },
                onQueryChange = setSearchTerm,
                trailingIcon = {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { search(searchTerm) }
                    ) {
                        AsyncImage(
                            model = R.drawable.ic_search,
                            contentDescription = stringResource(R.string.search_icon_description),
                            alignment = Alignment.Center,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            )
        },
        content = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(0.dp, 0.dp, 0.dp, 8.dp)
    )
}

@Preview
@Composable
fun SearchBarPreview() {
    TopSearchBar(
        initialSearchTerm = "Search",
        onSearch = {}
    )
}
