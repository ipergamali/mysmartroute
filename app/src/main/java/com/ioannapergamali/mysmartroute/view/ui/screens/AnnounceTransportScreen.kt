package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlinx.coroutines.launch
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress

private fun iconForVehicle(type: VehicleType): ImageVector = when (type) {
    VehicleType.CAR, VehicleType.TAXI -> Icons.Default.DirectionsCar
    VehicleType.BIGBUS, VehicleType.SMALLBUS -> Icons.Default.DirectionsBus
    VehicleType.BICYCLE -> Icons.Default.DirectionsBike
    VehicleType.MOTORBIKE -> Icons.Default.TwoWheeler
}

private fun formatAddress(address: PoiAddress): String = buildString {
    if (address.streetName.isNotBlank()) append(address.streetName)
    if (address.streetNum != 0) append(" ${address.streetNum}")
    if (address.postalCode != 0 || address.city.isNotBlank()) {
        if (isNotEmpty()) append(", ")
        append("${address.postalCode} ${address.city}".trim())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeViewModel: RouteViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val role by authViewModel.currentUserRole.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    var filteredVehicles by remember { mutableStateOf<List<VehicleEntity>>(emptyList()) }

    var displayRoutes by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }

    var expandedRoute by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var selectedVehicleName by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(0) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(routes, vehicles, selectedVehicle, selectedRouteId) {
        displayRoutes = if (selectedVehicle == VehicleType.BIGBUS) {
            routes.filter { route ->
                val pois = routeViewModel.getRoutePois(context, route.id)
                pois.all { it.type == Place.Type.BUS_STATION }
            }
        } else {
            routes
        }

        val isBusRoute = selectedRouteId?.let { id ->
            val poisSel = routeViewModel.getRoutePois(context, id)
            poisSel.all { it.type == Place.Type.BUS_STATION }
        } ?: false

        filteredVehicles = if (isBusRoute) {
            // Εάν η επιλεγμένη διαδρομή αποτελείται αποκλειστικά από στάσεις
            // λεωφορείων, επιτρέπουμε μόνο την επιλογή μεγάλου λεωφορείου.
            vehicles.filter { VehicleType.valueOf(it.type) == VehicleType.BIGBUS }
        } else {
            // Σε κάθε άλλη περίπτωση εμφανίζουμε όλα τα οχήματα ώστε ο χρήστης
            // να μπορεί να επιλέξει ελεύθερα.
            vehicles
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
    }

    LaunchedEffect(role) {
        val admin = role == UserRole.ADMIN
        routeViewModel.loadRoutes(context, includeAll = admin)
        vehicleViewModel.loadRegisteredVehicles(context, includeAll = admin)
    }

    fun refreshRoute() {
        val rId = selectedRouteId
        val vehicle = selectedVehicle
        if (rId != null && vehicle != null) {
            scope.launch {
                val (dur, path) = routeViewModel.getRouteDirections(context, rId, vehicle)
                duration = dur
                pathPoints = path
                pois = routeViewModel.getRoutePois(context, rId)
                path.firstOrNull()?.let { first ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(first, 13f))
                }
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
                            refreshRoute()
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = expandedVehicle, onExpandedChange = { expandedVehicle = !expandedVehicle }) {
                OutlinedTextField(
                    value = selectedVehicleName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.vehicle)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true
                )
                ExposedDropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                    filteredVehicles.forEach { vehicle ->
                        DropdownMenuItem(text = { Text(vehicle.name) }, onClick = {
                            selectedVehicle = VehicleType.valueOf(vehicle.type)
                            selectedVehicleName = vehicle.name
                            expandedVehicle = false
                            refreshRoute()
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedRouteId != null && selectedVehicle != null) {
                val routeName = displayRoutes.firstOrNull { it.id == selectedRouteId }?.name ?: ""
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = iconForVehicle(selectedVehicle!!),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$selectedVehicleName - $routeName", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(16.dp))
            }

            if (pathPoints.isNotEmpty() || pois.isNotEmpty()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    if (pathPoints.isNotEmpty()) {
                        Polyline(points = pathPoints)
                    }
                    pois.forEach { poi ->
                        Marker(
                            state = MarkerState(position = LatLng(poi.lat, poi.lng)),
                            title = poi.name
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            if (pois.isNotEmpty()) {
                Text(stringResource(R.string.stops_header))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.poi_name),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            stringResource(R.string.poi_type),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Divider()
                    pois.forEachIndexed { index, poi ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${index + 1}. ${poi.name}",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                poi.type.name,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

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
