package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import com.ioannapergamali.mysmartroute.utils.ATHENS_ZONE_ID
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Εμφανίζει τις μετακινήσεις κατηγοριοποιημένες ανάλογα με την
 * [com.ioannapergamali.mysmartroute.data.local.MovingStatus].
 */
@Composable
fun MovingListScreen(movings: List<MovingEntity>, now: Long = System.currentTimeMillis()) {
    val categorized = categorizeMovings(movings, now)
    LazyColumn {
        categorized.forEach { (status, list) ->
            item {
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(list) { moving ->
                Text(
                    "Μετακίνηση ${moving.id} – " +
                        "Ημερομηνία: ${formatDate(moving.date)} – " +
                        "Ώρα: ${formatTime(moving.date)} – " +
                        "Κόστος: ${formatCost(moving.cost)} – " +
                        "Διάρκεια: ${formatDuration(moving.durationMinutes)}"
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ATHENS_ZONE_ID)
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ATHENS_ZONE_ID)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatCost(cost: Double?): String =
    cost?.let { String.format(Locale.getDefault(), "%.2f€", it) } ?: "-"

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) String.format(Locale.getDefault(), "%d:%02d", hours, mins)
    else String.format(Locale.getDefault(), "0:%02d", mins)
}
