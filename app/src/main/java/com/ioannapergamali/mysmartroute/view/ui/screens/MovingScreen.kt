package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioannapergamali.mysmartroute.viewmodel.MovingViewModel
import com.ioannapergamali.mysmartroute.data.local.MovingStatus
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Οθόνη που εμφανίζει τις μετακινήσεις ομαδοποιημένες ανά κατάσταση.
 */
@Composable
fun MovingScreen(viewModel: MovingViewModel = hiltViewModel()) {
    val movings by viewModel.state.collectAsState()
    val grouped = categorizeMovings(movings)

    LazyColumn {
        MovingStatus.values().forEach { status ->
            val list = grouped[status].orEmpty()
            item {
                Text(
                    text = titleFor(status),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (list.isEmpty()) {
                item { Text("Δεν βρέθηκαν μετακινήσεις") }
            } else {
                items(list) { moving ->
                    Text("Μετακίνηση ${moving.id} – ${formatDate(moving.date)}")
                }
            }
        }
    }
}

private fun titleFor(status: MovingStatus) = when (status) {
    MovingStatus.ACTIVE -> "Ενεργές μετακινήσεις"
    MovingStatus.PENDING -> "Εκκρεμείς μετακινήσεις"
    MovingStatus.UNSUCCESSFUL -> "Ανεπιτυχείς μετακινήσεις"
    MovingStatus.COMPLETED -> "Ολοκληρωμένες μετακινήσεις"
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
