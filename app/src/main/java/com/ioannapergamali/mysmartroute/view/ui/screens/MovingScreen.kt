package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.viewmodel.MovingViewModel
import com.ioannapergamali.mysmartroute.data.local.MovingStatus
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import com.ioannapergamali.mysmartroute.viewmodel.AppDateTimeViewModel
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
    val context = LocalContext.current
    val dateViewModel: AppDateTimeViewModel = viewModel()
    val storedMillis by dateViewModel.dateTime.collectAsState()

    LaunchedEffect(Unit) { dateViewModel.load(context) }

    val now = storedMillis ?: System.currentTimeMillis()
    Log.d(TAG, "Σύνολο μετακινήσεων: ${movings.size}")

    val grouped = categorizeMovings(movings, now)

    LazyColumn {
        listOf(
            MovingStatus.PENDING,
            MovingStatus.UNSUCCESSFUL,
            MovingStatus.COMPLETED
        ).forEach { status ->
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
    MovingStatus.PENDING -> "Εκκρεμείς μετακινήσεις"
    MovingStatus.UNSUCCESSFUL -> "Ανεπιτυχείς μετακινήσεις"
    MovingStatus.COMPLETED -> "Ολοκληρωμένες μετακινήσεις"
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
