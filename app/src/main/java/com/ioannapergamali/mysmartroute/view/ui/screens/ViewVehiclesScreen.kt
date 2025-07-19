package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
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
            if (vehicles.isEmpty()) {

            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (driverId, vList) ->
                        val driverName = driverNames[driverId] ?: ""
                        item {
                            Text(
                                text = driverName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(Modifier.width(24.dp))
                                Text(text = stringResource(R.string.vehicle_name), modifier = Modifier.weight(1f))
                                Text(text = stringResource(R.string.license_plate), modifier = Modifier.weight(1f))
                                Text(text = stringResource(R.string.poi_description), modifier = Modifier.weight(1f))
                                Text(text = stringResource(R.string.vehicle_color), modifier = Modifier.weight(1f))
                                Text(text = stringResource(R.string.seats_label), modifier = Modifier.weight(1f))
                            }
                        }
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

@Composable
private fun VehicleRow(vehicle: VehicleEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val type = runCatching { VehicleType.valueOf(vehicle.type) }.getOrNull()
        type?.let {
            Icon(imageVector = iconForVehicle(it), contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = vehicle.name, modifier = Modifier.weight(1f))
        Text(text = vehicle.plate, modifier = Modifier.weight(1f))
        Text(text = vehicle.description, modifier = Modifier.weight(1f))
        Text(text = vehicle.color, modifier = Modifier.weight(1f))
        Text(text = vehicle.seat.toString(), modifier = Modifier.weight(1f))
    }
}
