package us.mikeandwan.photos.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import us.mikeandwan.photos.database.DeveloperLog

@Composable
fun DeveloperLogsScreen(
    logs: List<DeveloperLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Developer Logs",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onClearLogs) {
                Text("Clear")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No logs captured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(logs) { log ->
                    LogEntry(log)
                }
            }
        }
    }
}

@Composable
private fun LogEntry(log: DeveloperLog) {
    val timestamp = formatTimestamp(log.timestamp)
    val levelColor = if (log.level == "ERROR") Color.Red else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ).padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = log.level,
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
            Text(
                text = timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }

        Text(
            text = log.message,
            fontFamily = FontFamily.Default,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        if (!log.throwable.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall,
                    ).padding(8.dp)
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = log.throwable,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Red.copy(alpha = 0.8f),
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${
        localDateTime.minute.toString().padStart(2, '0')
    }:${localDateTime.second.toString().padStart(2, '0')}.${
        (localDateTime.nanosecond / 1_000_000).toString().padStart(3, '0')
    }"
}
