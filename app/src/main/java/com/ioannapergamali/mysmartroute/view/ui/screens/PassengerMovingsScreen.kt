package com.ioannapergamali.mysmartroute.view.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
    val context = LocalContext.current
    if (list.isNotEmpty()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                TableCell(stringResource(R.string.route))
                TableCell(stringResource(R.string.driver))
                TableCell(stringResource(R.string.vehicle_name))
                TableCell(stringResource(R.string.passenger))
                TableCell(stringResource(R.string.date))
                TableCell(stringResource(R.string.cost))
                TableCell(stringResource(R.string.duration))
            }
            list.forEach { m ->
                val dateText = if (m.date > 0L) {
                    DateFormat.getDateFormat(context).format(Date(m.date))
                } else ""
                Row {
                    TableCell(m.routeName)
                    TableCell(m.driverName)
                    TableCell(m.vehicleName)
                    TableCell(m.createdByName)
                    TableCell(dateText)
                    TableCell(String.format(Locale.getDefault(), "%.2f€", m.cost))
                    TableCell(m.durationMinutes.toString())
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TableCell(text: String) {
    Text(
        text,
        modifier = Modifier
            .width(120.dp)
            .padding(4.dp),
    )
}
