package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel

private fun iconForVehicle(type: VehicleType): ImageVector = when (type) {
    VehicleType.CAR, VehicleType.TAXI -> Icons.Default.DirectionsCar
    VehicleType.BIGBUS, VehicleType.SMALLBUS -> Icons.Default.DirectionsBus
    VehicleType.BICYCLE -> Icons.Default.DirectionsBike
    VehicleType.MOTORBIKE -> Icons.Default.TwoWheeler
}

@Composable
private fun TableCell(width: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)

            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
private fun HeaderRow() {
    Row {
        TableCell(width = 80.dp) { Text("") }
        TableCell(width = 120.dp) { Text(stringResource(R.string.vehicle_name)) }
        TableCell(width = 120.dp) { Text(stringResource(R.string.license_plate)) }
        TableCell(width = 80.dp) { Text(stringResource(R.string.vehicle_color)) }
        TableCell(width = 60.dp) { Text(stringResource(R.string.seats_label)) }
    }
}

@Composable
fun ViewVehiclesScreen(navController: NavController, openDrawer: () -> Unit) {
    val vehicleViewModel: VehicleViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vehicleViewModel.loadRegisteredVehicles(context, includeAll = true)
        userViewModel.loadDrivers(context)
    }

    val driverNames = drivers.associate { it.id to "${it.name} ${it.surname}" }
    val grouped = vehicles.groupBy { it.userId }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_vehicles),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (vehicles.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        grouped.forEach { (driverId, vList) ->
                            val driverName = driverNames[driverId] ?: ""
                            item {
                                Text(
                                    text = driverName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            item { HeaderRow() }
                            items(vList) { vehicle ->
                                VehicleRow(vehicle)
                            }
                            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(vehicle: VehicleEntity) {
    Row {
        val type = runCatching { VehicleType.valueOf(vehicle.type) }.getOrNull()
        TableCell(width = 80.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                type?.let { Icon(imageVector = iconForVehicle(it), contentDescription = null) }
                Text(text = vehicle.description, style = MaterialTheme.typography.labelSmall, softWrap = false)
            }
        }
        TableCell(width = 120.dp) { Text(text = vehicle.name, softWrap = false) }
        TableCell(width = 120.dp) { Text(text = vehicle.plate, softWrap = false) }
        TableCell(width = 80.dp) { Text(text = vehicle.color, softWrap = false) }
        TableCell(width = 60.dp) { Text(text = vehicle.seat.toString(), softWrap = false) }
    }
}
