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
import com.ioannapergamali.mysmartroute.viewmodel.AppDateTimeViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "MovingScreen"

/**
 * Οθόνη που εμφανίζει μόνο τις εκκρεμείς μετακινήσεις.
 */
@Composable
fun MovingScreen(viewModel: MovingViewModel = hiltViewModel()) {
    val movings by viewModel.state.collectAsState()
    val context = LocalContext.current
    val dateViewModel: AppDateTimeViewModel = viewModel()
    val storedMillis by dateViewModel.dateTime.collectAsState()

    LaunchedEffect(Unit) { dateViewModel.load(context) }
    LaunchedEffect(storedMillis) {
        viewModel.load(storedMillis ?: System.currentTimeMillis())
    }

    Log.d(TAG, "Εκκρεμείς μετακινήσεις: ${movings.size}")

    LazyColumn {
        item {
            Text(
                text = "Εκκρεμείς μετακινήσεις",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (movings.isEmpty()) {
            item { Text("Δεν βρέθηκαν μετακινήσεις") }
        } else {
            items(movings) { moving ->
                Log.d(TAG, "Εμφάνιση μετακίνησης ${moving.id} με ημερομηνία ${formatDate(moving.date)}")
                Text("Μετακίνηση ${moving.id} – ${formatDate(moving.date)}")
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
