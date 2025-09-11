package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
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

private const val TAG = "MovingScreen"

/**
 * Οθόνη που εμφανίζει τις μετακινήσεις ομαδοποιημένες ανά κατάσταση.
 */
@Composable
fun MovingScreen(viewModel: MovingViewModel = hiltViewModel()) {
    val movings by viewModel.state.collectAsState()
    Log.d(TAG, "Σύνολο μετακινήσεων: ${movings.size}")

    val grouped = categorizeMovings(movings)

    LazyColumn {
        MovingStatus.values().forEach { status ->
            val list = grouped[status].orEmpty()

            Log.d(TAG, "Κατηγορία $status περιέχει ${list.size} εγγραφές")

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

                    Log.d(TAG, "Εμφάνιση μετακίνησης ${moving.id} με ημερομηνία ${formatDate(moving.date)}")
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
