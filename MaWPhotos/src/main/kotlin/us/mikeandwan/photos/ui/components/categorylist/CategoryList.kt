package us.mikeandwan.photos.ui.components.categorylist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.mikeandwan.photos.domain.models.Category

@Composable
fun CategoryList(
    categories: List<Category>,
    showYear: Boolean,
    onSelectCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((Category) -> Unit)? = null,
    // hoisted for the callers that need to watch it - a paged listing asks for the next page from
    // how far it has been scrolled
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            categories,
            key = { _, item -> item.id },
        ) { index, category ->
            Column(modifier = Modifier.animateItem()) {
                CategoryListItem(
                    category = category,
                    showYear = showYear,
                    onSelectCategory = onSelectCategory,
                    onToggleFavorite = onToggleFavorite,
                )

                if (index != categories.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}
