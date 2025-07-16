package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import androidx.compose.ui.platform.LocalContext
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeViewModel: RouteViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()

    var displayRoutes by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }

    var expandedRoute by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var selectedVehicleDesc by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(0) }

    LaunchedEffect(routes, selectedVehicle) {
        displayRoutes = if (selectedVehicle == VehicleType.BIGBUS || selectedVehicle == VehicleType.SMALLBUS) {
            routes.filter { route ->
                val pois = routeViewModel.getRoutePois(context, route.id)
                pois.all { it.type == Place.Type.BUS_STATION }
            }
        } else {
            routes
        }
    }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context)
        vehicleViewModel.loadRegisteredVehicles(context)
    }

    fun refreshDuration() {
        val rId = selectedRouteId
        val vehicle = selectedVehicle
        if (rId != null && vehicle != null) {
            scope.launch {
                duration = routeViewModel.getRouteDuration(context, rId, vehicle)
            }
        }
    }

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.announce_availability),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            ExposedDropdownMenuBox(expanded = expandedRoute, onExpandedChange = { expandedRoute = !expandedRoute }) {
                OutlinedTextField(
                    value = displayRoutes.firstOrNull { it.id == selectedRouteId }?.name ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoute) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true
                )
                ExposedDropdownMenu(expanded = expandedRoute, onDismissRequest = { expandedRoute = false }) {
                    displayRoutes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRouteId = route.id
                            expandedRoute = false
                            refreshDuration()
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = expandedVehicle, onExpandedChange = { expandedVehicle = !expandedVehicle }) {
                OutlinedTextField(
                    value = selectedVehicleDesc,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.vehicle)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true
                )
                ExposedDropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(text = { Text(vehicle.description) }, onClick = {
                            selectedVehicle = VehicleType.valueOf(vehicle.type)
                            selectedVehicleDesc = vehicle.description
                            expandedVehicle = false
                            refreshDuration()
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text(stringResource(R.string.cost)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.duration_format, duration))

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val routeId = selectedRouteId
                    val vehicle = selectedVehicle
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    if (routeId != null && vehicle != null) {
                        declarationViewModel.declareTransport(context, routeId, vehicle, cost, duration)
                        navController.popBackStack()
                    }
                },
                enabled = selectedRouteId != null && selectedVehicle != null
            ) {
                Text(stringResource(R.string.announce))
            }
        }
    }
}
