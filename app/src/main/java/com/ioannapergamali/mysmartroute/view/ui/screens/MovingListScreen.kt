package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Εμφανίζει τις μετακινήσεις κατηγοριοποιημένες ανάλογα με την
 * [com.ioannapergamali.mysmartroute.data.local.MovingStatus].
 */
@Composable
fun MovingListScreen(movings: List<MovingEntity>) {
    val categorized = categorizeMovings(movings)
    LazyColumn {
        categorized.forEach { (status, list) ->
            item {
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(list) { moving ->
                Text("Μετακίνηση ${moving.id} – ${formatDate(moving.date)}")
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
