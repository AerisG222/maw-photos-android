package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import us.mikeandwan.photos.domain.models.Comment

@Composable
fun CommentTable(
    comments: List<Comment>,
    footer: @Composable () -> Unit
) {
    val fmt = remember { LocalDateTime.Format { date(LocalDate.Formats.ISO) } }
    val bgHead = MaterialTheme.colorScheme.surfaceVariant
    val txtHead = MaterialTheme.colorScheme.onSurfaceVariant
    val bgRow = MaterialTheme.colorScheme.surface
    val txtRow = MaterialTheme.colorScheme.onSurface

    LazyColumn {
        itemsIndexed(comments) { index, comment ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .background(bgHead)
                .padding(4.dp, 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    color = txtHead,
                    text = comment.createdBy
                )
                Text(
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    color = txtHead,
                    textAlign = TextAlign.End,
                    text = comment.created
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(fmt)
                )
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .background(bgRow)
                .padding(4.dp, 2.dp)
            ) {
                Text(
                    color = txtRow,
                    text = comment.body
                )
            }

            if (index != comments.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }

        item {
            footer()
        }
    }
}
