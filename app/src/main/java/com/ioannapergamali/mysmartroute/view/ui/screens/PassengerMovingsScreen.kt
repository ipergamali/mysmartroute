package com.ioannapergamali.mysmartroute.view.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ioannapergamali.mysmartroute.data.local.movingStatus
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMovingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VehicleRequestViewModel = viewModel()
    val movings by viewModel.movings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRequests(context)
    }

    val now = System.currentTimeMillis()
    val grouped = movings.groupBy { it.movingStatus(now) }

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
    if (list.isNotEmpty()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        list.forEach { m ->
            MovingItem(m)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MovingItem(m: MovingEntity) {
    val context = LocalContext.current
    val dateText = if (m.date > 0L) {
        DateFormat.getDateFormat(context).format(Date(m.date))
    } else ""
    val routeText = m.routeName.ifBlank { m.routeId }
    val driverText = m.driverName.ifBlank { m.driverId }
    val vehicleText = m.vehicleName.ifBlank { m.vehicleId }
    val passengerText = m.createdByName.ifBlank { m.userId }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("${stringResource(R.string.route)}: $routeText")
        Text("${stringResource(R.string.driver)}: $driverText")
        Text("${stringResource(R.string.vehicle_name)}: $vehicleText")
        Text("${stringResource(R.string.passenger)}: $passengerText")
        Text("${stringResource(R.string.date)}: $dateText")
        Text("${stringResource(R.string.cost)}: ${String.format(Locale.getDefault(), "%.2f€", m.cost)}")
        Text("${stringResource(R.string.duration)}: ${m.durationMinutes}")
    }
}
