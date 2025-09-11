package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MovingStatus
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "PassengerMovingsScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMovingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleRequestViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val role by authViewModel.currentUserRole.collectAsState()
    val movings by viewModel.movings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(role) {
        val isAdmin = role == UserRole.ADMIN
        viewModel.loadPassengerMovings(context, allUsers = isAdmin)
    }


    Log.d(TAG, "Φόρτωση ${movings.size} μετακινήσεων επιβάτη")
    val grouped = categorizeMovings(movings).also { map ->
        map.forEach { (status, list) ->
            Log.d(TAG, "Κατηγορία $status έχει ${list.size} εγγραφές")

        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_movings),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (movings.isEmpty()) {
                Text(stringResource(R.string.no_movings))
            } else {
                MovingCategory(
                    stringResource(R.string.active_movings),
                    grouped[MovingStatus.ACTIVE].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.pending_movings),
                    grouped[MovingStatus.PENDING].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.unsuccessful_movings),
                    grouped[MovingStatus.UNSUCCESSFUL].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.completed_movings),
                    grouped[MovingStatus.COMPLETED].orEmpty()
                )
            }
        }
    }
}

@Composable
private fun MovingCategory(title: String, list: List<MovingEntity>) {

    Log.d(TAG, "Εμφάνιση κατηγορίας $title με ${list.size} εγγραφές")

    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    MovingTable(list)
    if (list.isEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.no_movings))
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MovingTable(list: List<MovingEntity>) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scrollState)
    ) {
        Column {
            Row {
                TableHeaderCell(stringResource(R.string.route))
                TableHeaderCell(stringResource(R.string.driver))
                TableHeaderCell(stringResource(R.string.vehicle_name))
                TableHeaderCell(stringResource(R.string.passenger))
                TableHeaderCell(stringResource(R.string.date))
                TableHeaderCell(stringResource(R.string.time))
                TableHeaderCell(stringResource(R.string.cost))
                TableHeaderCell(stringResource(R.string.duration))
            }
            Divider()
            list.forEach { m ->

                Log.d(TAG, "Γραμμή για μετακίνηση ${m.id} με status ${m.status}")

                Row {
                    TableCell(m.routeName.ifBlank { "-" })
                    TableCell(m.driverName.ifBlank { "-" })
                    TableCell(m.vehicleName.ifBlank { "-" })
                    TableCell(m.createdByName.ifBlank { "-" })
                    TableCell(formatDate(m.date))
                    TableCell(formatTime(m.date))
                    TableCell(formatCost(m.cost))
                    TableCell(formatDuration(m.durationMinutes))
                }
                Divider()
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
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

@Composable
private fun RowScope.TableHeaderCell(text: String) {
    Text(
        text,
        modifier = Modifier
            .width(120.dp)
            .padding(4.dp),
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun RowScope.TableCell(text: String) {
    Text(
        text,
        modifier = Modifier
            .width(120.dp)
            .padding(4.dp)
    )
}
