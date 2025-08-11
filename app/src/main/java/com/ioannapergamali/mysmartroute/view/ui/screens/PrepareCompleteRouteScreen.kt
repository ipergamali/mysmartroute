package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast
import android.app.TimePickerDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.util.iconForVehicle
import com.ioannapergamali.mysmartroute.view.ui.util.labelForVehicle
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareCompleteRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val reservationViewModel: ReservationViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val reservations by reservationViewModel.reservations.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val role by authViewModel.currentUserRole.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    val poiNames = remember { mutableStateMapOf<String, String>() }
    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedDeclaration by remember { mutableStateOf<TransportDeclarationEntity?>(null) }
    var selectedTime by remember { mutableStateOf<Long?>(null) }
    var selectedTimeText by remember { mutableStateOf("") }

    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var vehicleName by remember { mutableStateOf("") }
    var selectedVehicleId by remember { mutableStateOf("") }
    var selectedVehicleDescription by remember { mutableStateOf("") }
    var expandedVehicle by remember { mutableStateOf(false) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var selectedDriverName by remember { mutableStateOf("") }
    var displayRoutes by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
        declarationViewModel.loadDeclarations(context)
        poiViewModel.loadPois(context)
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

    LaunchedEffect(role, drivers) {
        if (role != UserRole.ADMIN && selectedDriverName.isBlank()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                selectedDriverId = uid
                selectedDriverName = userViewModel.getUserName(context, uid)
            }
        }
    }

    LaunchedEffect(routes, selectedDriverId) {
        displayRoutes = selectedDriverId?.let { id ->
            routes.filter { it.userId == id }
        } ?: routes
    }

    LaunchedEffect(selectedDriverId) {
        selectedDriverId?.let { vehicleViewModel.loadRegisteredVehicles(context, userId = it) }
    }

    LaunchedEffect(selectedRoute, selectedDate, selectedTime, declarations) {
        selectedRoute?.let { route ->
            val (_, path) = routeViewModel.getRouteDirections(context, route.id, VehicleType.CAR)
            pathPoints = path
            pois = routeViewModel.getRoutePois(context, route.id)

            val date = selectedDate ?: 0L
            val time = selectedTime ?: 0L
            val decl = declarations.find { it.routeId == route.id && it.date == date && it.startTime == time }
            selectedDeclaration = decl
            val declId = decl?.id ?: ""
            reservationViewModel.loadReservations(context, route.id, date, time, declId)

            path.firstOrNull()?.let {
                MapsInitializer.initialize(context)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }

    LaunchedEffect(selectedDeclaration, vehicles) {
        val decl = selectedDeclaration
        if (decl != null) {
            selectedVehicle = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
            selectedVehicleId = decl.vehicleId
            vehicles.firstOrNull { it.id == decl.vehicleId }?.let { veh ->
                vehicleName = veh.name
                selectedVehicleDescription = veh.description
            }
        }
    }

    LaunchedEffect(reservations) {
        reservations.forEach { res ->
            if (res.userId.isNotBlank() && userNames[res.userId] == null) {
                userNames[res.userId] = userViewModel.getUserName(context, res.userId)
            }
            if (res.startPoiId.isNotBlank() && poiNames[res.startPoiId] == null) {
                poiNames[res.startPoiId] = poiViewModel.getPoiName(context, res.startPoiId)
            }
            if (res.endPoiId.isNotBlank() && poiNames[res.endPoiId] == null) {
                poiNames[res.endPoiId] = poiViewModel.getPoiName(context, res.endPoiId)
            }
        }
    }


    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.prepare_complete_route),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
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
                            DropdownMenuItem(
                                text = { Text("${driver.name} ${driver.surname}") },
                                onClick = {
                                    selectedDriverId = driver.id
                                    selectedDriverName = "${driver.name} ${driver.surname}"
                                    expandedDriver = false
                                    selectedRoute = null
                                    selectedDate = null
                                    selectedTime = null
                                }
                            )
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

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.route_name)) },
                    placeholder = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    displayRoutes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRoute = route
                            expanded = false
                            selectedDate = null
                            selectedTime = null

                        })
                    }
                }
            }

            val datePickerState = rememberDatePickerState()
            var showDatePicker by remember { mutableStateOf(false) }
            val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy") }
            val selectedDateText = selectedDate?.let { millis ->
                java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            } ?: stringResource(R.string.select_date)

            if (selectedRoute != null) {
                Text(stringResource(R.string.departure_date))
                Button(onClick = { showDatePicker = true }) { Text(selectedDateText) }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedDate = datePickerState.selectedDateMillis
                                selectedTime = null

                                showDatePicker = false
                            }) {
                                Text(stringResource(android.R.string.ok))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (selectedRoute != null && selectedDate != null) {
                Text(stringResource(R.string.time))
                Button(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            selectedTime = (hour * 60 + minute) * 60_000L
                            selectedTimeText = String.format("%02d:%02d", hour, minute)
                        },
                        0,
                        0,
                        true
                    ).show()
                }) {
                    Text(if (selectedTimeText.isNotEmpty()) selectedTimeText else stringResource(R.string.select_time))
                }
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.vehicle_type))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleType.values().forEach { type ->
                        val isSelected = selectedVehicle == type
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    selectedVehicle = if (isSelected) null else type
                                    vehicleName = ""
                                    selectedVehicleId = ""
                                    selectedVehicleDescription = ""
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                      else ComposeColor.Transparent
                                )
                                .border(
                                    2.dp,
                                      if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.Transparent,
                                    CircleShape
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = iconForVehicle(type),
                                contentDescription = labelForVehicle(type),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(labelForVehicle(type), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(expanded = expandedVehicle, onExpandedChange = { expandedVehicle = !expandedVehicle }) {
                    OutlinedTextField(
                        value = vehicleName,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.vehicle_name)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true
                    )
                    ExposedDropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                        vehicles
                            .filter {
                                it.userId == selectedDriverId &&
                                    (selectedVehicle == null || runCatching { VehicleType.valueOf(it.type) }.getOrNull() == selectedVehicle)
                            }
                            .forEach { vehicle ->
                                DropdownMenuItem(text = { Text(vehicle.name) }, onClick = {
                                    vehicleName = vehicle.name
                                    selectedVehicle = runCatching { VehicleType.valueOf(vehicle.type) }.getOrNull()
                                    selectedVehicleId = vehicle.id
                                    selectedVehicleDescription = vehicle.description
                                    expandedVehicle = false
                                })
                            }
                    }
                }
                if (selectedVehicleDescription.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        selectedVehicle?.let { Icon(iconForVehicle(it), contentDescription = null) }
                        Spacer(Modifier.width(8.dp))
                        Text(selectedVehicleDescription)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            val seats = selectedDeclaration?.seats ?: 0

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints)
                    pois.forEach { poi ->
                        Marker(state = MarkerState(position = LatLng(poi.lat, poi.lng)), title = poi.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else if (isKeyMissing) {
                Text(stringResource(R.string.map_api_key_missing))
                Spacer(Modifier.height(16.dp))
            }

            if (seats > 0) {
                Text(stringResource(R.string.available_seats, seats - reservations.size))
                Spacer(Modifier.height(16.dp))
            }

            if (reservations.isNotEmpty()) {
                Text(stringResource(R.string.print_list))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.passenger), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.boarding_stop), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.dropoff_stop), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    Divider()
                    reservations.forEach { res ->
                        val userName = userNames[res.userId] ?: ""
                        val startName = poiNames[res.startPoiId] ?: ""
                        val endName = poiNames[res.endPoiId] ?: ""

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(userName, modifier = Modifier.weight(1f))
                            Text(startName, modifier = Modifier.weight(1f))
                            Text(endName, modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (selectedRoute != null && selectedDate != null) {
                Text(stringResource(R.string.no_reservations))
            }

            Spacer(Modifier.height(16.dp))
            if (selectedRoute != null && selectedDate != null) {
                Button(onClick = {
                    val decl = selectedDeclaration
                    if (decl != null) {
                        val updatedDecl = decl.copy(
                            vehicleId = selectedVehicleId.ifBlank { decl.vehicleId },
                            vehicleType = selectedVehicle?.name ?: decl.vehicleType
                        )
                        reservationViewModel.completeRoute(
                            context,
                            selectedRoute!!.id,
                            selectedDate!!,
                            updatedDecl.startTime,
                            updatedDecl
                        ) { completed ->
                            val msg = if (completed) R.string.route_completed else R.string.route_already_completed
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.complete_route))
                }
            }
        }
    }
}
