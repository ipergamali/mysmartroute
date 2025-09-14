package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import android.widget.Toast
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.FilledIconButton
import com.google.android.libraries.places.api.model.Place

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

private fun arePoisSequential(
    pois: List<PoIEntity>,
    details: List<TransportDeclarationDetailEntity>
): Boolean {
    if (details.size != pois.size - 1) return false
    val pairs = details.map { it.startPoiId to it.endPoiId }.toSet()
    return pois.zipWithNext().all { (a, b) -> pairs.contains(a.id to b.id) }
}

private fun canSend(
    routeSelected: Boolean,
    pois: List<PoIEntity>,
    details: List<TransportDeclarationDetailEntity>,
    startIndex: Int?,
    endIndex: Int?,
    cost: String,
    dateSelected: Boolean,
    timeSelected: Boolean,
    calculating: Boolean
): Boolean {
    if (!routeSelected || cost.isBlank() || !dateSelected || !timeSelected || calculating) return false
    if (details.isNotEmpty()) {
        if (arePoisSequential(pois, details)) return true
        val firstId = pois.firstOrNull()?.id
        val lastId = pois.lastOrNull()?.id
        if (firstId != null && lastId != null) {
            return details.any { it.startPoiId == firstId && it.endPoiId == lastId }
        }
        return false
    }
    return startIndex == 0 && endIndex == pois.lastIndex
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
    var segmentDuration by remember { mutableStateOf(0) }
    val detailDurations = remember { mutableStateListOf<Int>() }
    val duration by remember { derivedStateOf { detailDurations.sum() + segmentDuration } }
    var calculating by remember { mutableStateOf(false) }
    val now = LocalDateTime.now()
    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayMillis
        }
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)
    val timePickerState = rememberTimePickerState(
        initialHour = now.hour,
        initialMinute = now.minute
    )
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedTimeText = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
    LaunchedEffect(datePickerState.selectedDateMillis) {
        if (datePickerState.selectedDateMillis == todayMillis) {
            val currentTime = LocalTime.now()
            timePickerState.hour = currentTime.hour
            timePickerState.minute = currentTime.minute
        }
    }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var startIndex by remember { mutableStateOf<Int?>(null) }
    var endIndex by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    val details = remember { mutableStateListOf<TransportDeclarationDetailEntity>() }
    var minSeats by remember { mutableStateOf(Int.MAX_VALUE) }
    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    fun refreshRoute() {
        val rId = selectedRouteId
        val vehicle = selectedVehicle
        if (rId != null && vehicle != null) {
            scope.launch {
                calculating = true
                val poisList = routeViewModel.getRoutePois(context, rId)
                pois = poisList
                val fromIdx = startIndex ?: 0
                val toIdx = endIndex ?: poisList.lastIndex
                val origin = LatLng(poisList[fromIdx].lat, poisList[fromIdx].lng)
                val destination = LatLng(poisList[toIdx].lat, poisList[toIdx].lng)
                val waypoints = if (toIdx - fromIdx > 1) {
                    poisList.subList(fromIdx + 1, toIdx).map { LatLng(it.lat, it.lng) }
                } else emptyList()
                val data = MapsUtils.fetchDurationAndPath(origin, destination, apiKey, vehicle, waypoints)
                segmentDuration = data.duration
                pathPoints = if (data.points.isNotEmpty()) data.points else poisList.map { LatLng(it.lat, it.lng) }
                pathPoints.firstOrNull()?.let { first ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(first, 13f))
                }
                calculating = false
            }
        }
    }

    LaunchedEffect(routes, selectedVehicle, selectedRouteId, selectedDriverId, role) {
        val driverFiltered = if (role == UserRole.ADMIN) {
            routes
        } else {
            selectedDriverId?.let { id ->
                routes.filter { it.userId == id }
            } ?: routes
        }
        displayRoutes = driverFiltered

        if (selectedRouteId != null && selectedVehicle != null) {
            refreshRoute()
        }
    }


    LaunchedEffect(vehicles, selectedDriverId, selectedRouteId) {

        var list = vehicles
        selectedDriverId?.let { id -> list = list.filter { it.userId == id } }
        filteredVehicles = list
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
                                selectedVehicleId = ""
                                selectedVehicleName = ""
                                selectedVehicleDescription = ""
                                selectedVehicleSeats = 0
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
                            selectedVehicle = null
                            selectedVehicleId = ""
                            selectedVehicleName = ""
                            selectedVehicleDescription = ""
                            selectedVehicleSeats = 0
                            startIndex = null
                            endIndex = null
                            message = ""
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
                    offsetPois(pois).forEach { (poi, position) ->
                        Marker(
                            state = MarkerState(position = position),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}. ${poi.name}",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                poi.type.name,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (endIndex == null || index < endIndex!!) {
                                    startIndex = index
                                    refreshRoute()
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83C\uDD95")
                            }
                            IconButton(onClick = {
                                if (startIndex == null || index > startIndex!!) {
                                    endIndex = index
                                    refreshRoute()
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83D\uDD1A")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = startIndex?.let { "${it + 1}. ${pois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.boarding_stop)) },
                    leadingIcon = { Text("\uD83C\uDD95") },
                    trailingIcon = {
                        IconButton(onClick = {
                            startIndex = null
                            refreshRoute()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.clear_selection))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = endIndex?.let { "${it + 1}. ${pois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dropoff_stop)) },
                    leadingIcon = { Text("\uD83D\uDD1A") },
                    trailingIcon = {
                        IconButton(onClick = {
                            endIndex = null
                            refreshRoute()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.clear_selection))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                FilledIconButton(onClick = {
                    val s = startIndex
                    val e = endIndex
                    val veh = selectedVehicle
                    if (s != null && e != null && veh != null) {
                        val startPoi = pois[s]
                        val endPoi = pois[e]
                        if (startPoi.type == Place.Type.BUS_STATION &&
                            endPoi.type == Place.Type.BUS_STATION &&
                            veh != VehicleType.BIGBUS &&
                            veh != VehicleType.SMALLBUS
                        ) {
                            message = context.getString(R.string.bus_required)
                        } else {
                            val detail = TransportDeclarationDetailEntity(
                                startPoiId = startPoi.id,
                                endPoiId = endPoi.id,
                                vehicleId = selectedVehicleId,
                                vehicleType = veh.name,
                                seats = selectedVehicleSeats
                            )
                            details.add(detail)
                            detailDurations.add(segmentDuration)
                            minSeats = kotlin.math.min(minSeats, selectedVehicleSeats)
                            startIndex = null
                            endIndex = null
                            segmentDuration = 0
                        }
                    } else {
                        message = context.getString(R.string.invalid_stop_order)
                    }
                }) {
                    Icon(Icons.Default.Link, contentDescription = stringResource(R.string.add))
                }

                if (details.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.associations_header))
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.boarding_stop), Modifier.weight(1f))
                            Text(stringResource(R.string.dropoff_stop), Modifier.weight(1f))
                            Text(stringResource(R.string.vehicle_name), Modifier.weight(1f))
                        }
                        Divider()
                        details.forEachIndexed { index, d ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    pois.firstOrNull { it.id == d.startPoiId }?.name ?: d.startPoiId,
                                    Modifier.weight(1f)
                                )
                                Text(
                                    pois.firstOrNull { it.id == d.endPoiId }?.name ?: d.endPoiId,
                                    Modifier.weight(1f)
                                )
                                Text(
                                    vehicles.firstOrNull { it.id == d.vehicleId }?.name ?: d.vehicleId,
                                    Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    details.removeAt(index)
                                    detailDurations.removeAt(index)
                                    minSeats = details.minOfOrNull { dt -> dt.seats } ?: Int.MAX_VALUE
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (message.isNotBlank()) {
                    Text(message)
                    Spacer(Modifier.height(16.dp))
                }
            }

            Button(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
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
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(selectedTimeText)
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val selectedDateMillis = datePickerState.selectedDateMillis ?: todayMillis
                            val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            val chosenDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            if (chosenDateTime.isBefore(LocalDateTime.now())) {
                                Toast.makeText(
                                    context,
                                    R.string.invalid_datetime,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showTimePicker = false
                            }
                        }) {
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

            val sendEnabled = remember(
                selectedRouteId,
                pois,
                details,
                startIndex,
                endIndex,
                costText,
                datePickerState.selectedDateMillis,
                timePickerState.hour,
                timePickerState.minute,
                calculating
            ) {
                canSend(
                    routeSelected = selectedRouteId != null,
                    pois = pois,
                    details = details.toList(),
                    startIndex = startIndex,
                    endIndex = endIndex,
                    cost = costText,
                    dateSelected = datePickerState.selectedDateMillis != null,
                    timeSelected = true,
                    calculating = calculating
                )
            }

            Button(
                onClick = {
                    val routeId = selectedRouteId
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val date = datePickerState.selectedDateMillis ?: 0L
                    val selectedDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val chosenDateTime = LocalDateTime.of(selectedDate, selectedTime)
                    val driverId = selectedDriverId ?: ""
                    if (routeId != null) {
                        if (chosenDateTime.isBefore(LocalDateTime.now())) {
                            Toast.makeText(
                                context,
                                R.string.invalid_datetime,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (details.isEmpty()) {
                            val s = startIndex ?: 0
                            val e = endIndex ?: pois.lastIndex
                            val veh = selectedVehicle
                            if (veh != null && s < pois.size && e < pois.size) {
                                val startPoi = pois[s]
                                val endPoi = pois[e]
                                if (startPoi.type == Place.Type.BUS_STATION && endPoi.type == Place.Type.BUS_STATION && veh != VehicleType.BIGBUS && veh != VehicleType.SMALLBUS) {
                                    Toast.makeText(context, R.string.bus_required, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                        }
                        val startTime = (timePickerState.hour * 60 + timePickerState.minute) * 60_000L
                        scope.launch {
                            val detailList = if (details.isNotEmpty()) {
                                details.toList()
                            } else {
                                val s = startIndex ?: 0
                                val e = endIndex ?: pois.lastIndex
                                val veh = selectedVehicle
                                if (veh != null && s < pois.size && e < pois.size) {
                                    listOf(
                                        TransportDeclarationDetailEntity(
                                            startPoiId = pois[s].id,
                                            endPoiId = pois[e].id,
                                            vehicleId = selectedVehicleId,
                                            vehicleType = veh.name,
                                            seats = selectedVehicleSeats
                                        )
                                    )
                                } else emptyList()
                            }
                            val minSeatsValue = if (details.isNotEmpty()) minSeats else selectedVehicleSeats
                            val costPerDetail = if (detailList.isNotEmpty()) cost / detailList.size else 0.0
                            val detailedCosts = detailList.map { it.copy(cost = costPerDetail) }
                            val success = declarationViewModel.declareTransport(
                                context,
                                routeId,
                                driverId,
                                if (minSeatsValue == Int.MAX_VALUE) 0 else minSeatsValue,
                                duration,
                                date,
                                startTime,
                                detailedCosts
                            )
                            Toast.makeText(
                                context,
                                if (success) R.string.declare_success else R.string.declare_failure,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (success) {
                                navController.popBackStack()
                            }
                        }
                    }
                },
                enabled = sendEnabled
            ) {
                Text(stringResource(R.string.announce))
            }
        }
    }
}
