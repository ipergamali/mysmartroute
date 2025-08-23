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
import androidx.compose.ui.res.dimensionResource
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
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState

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
    val userViewModel: UserViewModel = viewModel()
    val role by authViewModel.currentUserRole.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    var filteredVehicles by remember { mutableStateOf<List<VehicleEntity>>(emptyList()) }

    var displayRoutes by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }

    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var selectedDriverName by remember { mutableStateOf("") }
    var expandedRoute by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var selectedVehicleId by remember { mutableStateOf("") }
    var selectedVehicleName by remember { mutableStateOf("") }
    var selectedVehicleDescription by remember { mutableStateOf("") }
    var selectedVehicleSeats by remember { mutableStateOf(0) }
    var costText by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(0) }
    var calculating by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedTimeText = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    fun refreshRoute() {
        val rId = selectedRouteId
        val vehicle = selectedVehicle
        if (rId != null && vehicle != null) {
            scope.launch {
                calculating = true
                val (_, path) = routeViewModel.getRouteDirections(context, rId, vehicle)
                pathPoints = path
                pois = routeViewModel.getRoutePois(context, rId)
                duration = routeViewModel.getRouteDuration(context, rId, vehicle)
                path.firstOrNull()?.let { first ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(first, 13f))
                }
                calculating = false
            }
        }
    }

    LaunchedEffect(routes, vehicles, selectedVehicle, selectedRouteId, selectedDriverId) {
        val driverFiltered = selectedDriverId?.let { id ->
            routes.filter { it.userId == id }
        } ?: routes
        displayRoutes = if (selectedVehicle == VehicleType.BIGBUS) {
            driverFiltered.filter { route ->
                val pois = routeViewModel.getRoutePois(context, route.id)
                pois.isNotEmpty() && pois.all { it.type == Place.Type.BUS_STATION }
            }
        } else {
            driverFiltered
        }

        val isBusRoute = selectedRouteId?.let { id ->
            val poisSel = routeViewModel.getRoutePois(context, id)
            poisSel.isNotEmpty() && poisSel.all { it.type == Place.Type.BUS_STATION }
        } ?: false

        var list = vehicles
        selectedDriverId?.let { id -> list = list.filter { it.userId == id } }
        filteredVehicles = if (isBusRoute) {
            list.filter { VehicleType.valueOf(it.type) == VehicleType.BIGBUS }
        } else {
            list
        }

        if (selectedRouteId != null && selectedVehicle != null) {
            refreshRoute()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
    }

    LaunchedEffect(role) {
        val admin = role == UserRole.ADMIN
        routeViewModel.loadRoutes(context, includeAll = admin)
        if (admin) {
            userViewModel.loadDrivers(context)
            selectedDriverId = null
            selectedDriverName = ""
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                selectedDriverId = uid
                selectedDriverName = userViewModel.getUserName(context, uid)
            }
        }
    }

    LaunchedEffect(selectedDriverId) {
        selectedDriverId?.let { id ->
            vehicleViewModel.loadRegisteredVehicles(context, userId = id)
        }
    }

    LaunchedEffect(role, drivers) {
        if (role != UserRole.ADMIN && selectedDriverName.isBlank()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                selectedDriverId = uid
                selectedDriverName = userViewModel.getUserName(context, uid)
            }
        }
    }

    LaunchedEffect(filteredVehicles) {
        if (selectedVehicle == null && filteredVehicles.isNotEmpty()) {
            val first = filteredVehicles.first()
            selectedVehicle = VehicleType.valueOf(first.type)
            selectedVehicleId = first.id
            selectedVehicleName = first.name
            selectedVehicleDescription = first.description
            selectedVehicleSeats = first.seat
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
            val bubbleState = LocalKeyboardBubbleState.current!!

            if (role == UserRole.ADMIN) {
                ExposedDropdownMenuBox(expanded = expandedDriver, onExpandedChange = { expandedDriver = !expandedDriver }) {
                    OutlinedTextField(
                        value = selectedDriverName,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.driver)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDriver) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true
                    )
                    ExposedDropdownMenu(expanded = expandedDriver, onDismissRequest = { expandedDriver = false }) {
                        drivers.forEach { driver ->
                            DropdownMenuItem(text = { Text("${driver.name} ${driver.surname}") }, onClick = {
                                selectedDriverId = driver.id
                                selectedDriverName = "${driver.name} ${driver.surname}"
                                expandedDriver = false
                                selectedRouteId = null
                                selectedVehicle = null
                                selectedVehicleName = ""
                                selectedVehicleDescription = ""
                                vehicleViewModel.loadRegisteredVehicles(context, userId = driver.id)
                            })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                OutlinedTextField(
                    value = selectedDriverName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.driver)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                Spacer(Modifier.height(16.dp))
            }

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
                            selectedVehicleId = vehicle.id
                            selectedVehicleName = vehicle.name
                            selectedVehicleDescription = vehicle.description
                            selectedVehicleSeats = vehicle.seat
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
                    Text("$selectedVehicleDescription - $routeName", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(16.dp))
            }

            if (!isKeyMissing && (pathPoints.isNotEmpty() || pois.isNotEmpty())) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
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
            } else if (isKeyMissing) {
                Text(stringResource(R.string.map_api_key_missing))
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

            Button(onClick = { showDatePicker = true }) {
                Text(selectedDateText)
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { showTimePicker = true }) {
                Text(selectedTimeText)
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text(stringResource(R.string.cost)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 0) { costText },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.duration_format, duration))

            if (calculating) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val routeId = selectedRouteId
                    val vehicle = selectedVehicle
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val date = datePickerState.selectedDateMillis ?: 0L
                    val startTime = (timePickerState.hour * 60 + timePickerState.minute) * 60_000L
                    val driverId = selectedDriverId ?: ""
                    if (routeId != null && vehicle != null) {
                        declarationViewModel.declareTransport(
                            context,
                            routeId,
                            driverId,
                            selectedVehicleId,
                            vehicle,
                            selectedVehicleSeats,
                            cost,
                            duration,
                            date,
                            startTime
                        )
                        navController.popBackStack()
                    }
                },
                enabled = selectedRouteId != null && selectedVehicle != null && !calculating
            ) {
                Text(stringResource(R.string.announce))
            }
        }
    }
}
