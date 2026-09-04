package us.mikeandwan.photos.ui.components.categorylist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.findTeaserImage
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.ui.components.favorite.FavoriteIcon

@Composable
fun CategoryListItem(
    category: Category,
    showYear: Boolean,
    onSelectCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    onToggleFavorite: ((Category) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current

    @Composable
    fun getMediaTypeIconAlpha(hasType: Boolean): Float =
        if (hasType) {
            1f
        } else {
            0.2f
        }

    Row(
        modifier
            .fillMaxWidth()
            .clickable { onSelectCategory(category) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = category.findTeaserImage(largerSize = false).path,
            contentDescription = category.name,
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_broken_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(60.dp)
                .width(60.dp)
                .padding(2.dp),
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_round_camera),
            contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 2.dp, end = 4.dp)
                .size(24.dp)
                .alpha(getMediaTypeIconAlpha(category.mediaTypes.contains(MediaType.Photo))),
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_round_play_circle),
            contentDescription = stringResource(id = R.string.li_category_thumbnail_description),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(24.dp)
                .alpha(getMediaTypeIconAlpha(category.mediaTypes.contains(MediaType.Video))),
        )

        if (showYear) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = category.year.toString(),
                modifier = Modifier
                    .padding(8.dp),
            )
        }

        // the spacer stands in when the name is hidden, so the count and the heart stay where they
        // were rather than sliding in against the year
        if (showName) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = category.name,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // only the person and clan listings carry a count - there it is how much of the category
        // they are in, which is the whole reason for listing it.  everywhere else the category is
        // the thing being listed and there is nothing to compare it against.
        category.mediaCount?.let { count ->
            Text(
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (onToggleFavorite != null) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(
                        if (category.isFavorite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                    )
                    onToggleFavorite(category)
                },
            ) {
                FavoriteIcon(
                    isFavorite = category.isFavorite,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CategoryListItemPreview() {
    CategoryListItem(
        category = Category(
            id = Uuid.random(),
            name = "Test Category",
            year = 2024,
            isFavorite = false,
            effectiveDate = LocalDate(2024, 1, 1),
            modified = Instant.fromEpochMilliseconds(0),
            teaser = emptyList(),
            mediaTypes = listOf(MediaType.Photo, MediaType.Video),
        ),
        showYear = true,
        onSelectCategory = {},
        onToggleFavorite = {},
    )
}
