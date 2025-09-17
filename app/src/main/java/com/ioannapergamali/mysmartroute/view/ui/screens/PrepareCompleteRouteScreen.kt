package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.ATHENS_ZONE_ID
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.flow.firstOrNull
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.SessionManager
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
    val routes by routeViewModel.routes.collectAsState()
    val reservations by reservationViewModel.reservations.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val role by authViewModel.currentUserRole.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedDeclaration by remember { mutableStateOf<TransportDeclarationEntity?>(null) }
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
            val uid = SessionManager.currentUserId()
            if (uid != null) {
                selectedDriverId = uid
                selectedDriverName = userViewModel.getUserName(context, uid)
            }
        }
    }

    LaunchedEffect(role, drivers) {
        if (role != UserRole.ADMIN && selectedDriverName.isBlank()) {
            val uid = SessionManager.currentUserId()
            if (uid != null) {
                selectedDriverId = uid
                selectedDriverName = userViewModel.getUserName(context, uid)
            }
        }
    }

    LaunchedEffect(routes, declarations, selectedDriverId) {
        val filteredRoutes = selectedDriverId?.takeIf { it.isNotBlank() }?.let { driverId ->
            val declaredRouteIds = declarations
                .asSequence()
                .filter { it.driverId == driverId }
                .map { it.routeId }
                .toSet()
            routes.filter { it.id in declaredRouteIds }
        } ?: emptyList()

        displayRoutes = filteredRoutes
        if (selectedRoute?.id !in filteredRoutes.map { it.id }) {
            selectedRoute = null
            selectedDate = null
            selectedDeclaration = null
        }
    }

    LaunchedEffect(selectedRoute, selectedDate, declarations, selectedDriverId) {
        selectedRoute?.let { route ->
            val (_, path) = routeViewModel.getRouteDirections(context, route.id, VehicleType.CAR)
            pathPoints = path
            pois = routeViewModel.getRoutePois(context, route.id)

            val date = selectedDate ?: return@LaunchedEffect
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val count = dbLocal.movingDao().countPendingOrOpenForRoute(route.id, date)
            if (count > 0) {
                val driverId = selectedDriverId
                val decl = declarations.find {
                    it.routeId == route.id &&
                        it.date == date &&
                        (driverId == null || driverId.isBlank() || it.driverId == driverId)
                }
                selectedDeclaration = decl
                val declId = decl?.id ?: ""
                val startTime = decl?.startTime ?: 0L
                reservationViewModel.loadReservations(context, route.id, date, startTime, declId)
            } else {
                selectedDeclaration = null
                reservationViewModel.loadReservations(context, "", 0L, 0L, "")
            }

            path.firstOrNull()?.let {
                MapsInitializer.initialize(context)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }

    LaunchedEffect(reservations) {
        reservations.forEach { res ->
            if (res.userId.isNotBlank() && userNames[res.userId] == null) {
                userNames[res.userId] = userViewModel.getUserName(context, res.userId)
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

                        })
                    }
                }
            }

            val datePickerState = rememberDatePickerState()
            var showDatePicker by remember { mutableStateOf(false) }
            val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy") }
            val selectedDateText = selectedDate?.let { millis ->
                java.time.Instant.ofEpochMilli(millis).atZone(ATHENS_ZONE_ID).toLocalDate().format(dateFormatter)
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

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints)
                    offsetPois(pois).forEach { (poi, position) ->
                        Marker(state = MarkerState(position = position), title = poi.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else if (isKeyMissing) {
                Text(stringResource(R.string.map_api_key_missing))
                Spacer(Modifier.height(16.dp))
            }

            if (reservations.isNotEmpty()) {
                Text(stringResource(R.string.print_list))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.passenger),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Divider()
                    reservations.forEach { res ->
                        val userName = userNames[res.userId] ?: ""
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(userName)
                        }
                    }
                }
            } else if (selectedRoute != null && selectedDate != null && selectedDeclaration != null) {
                Text(stringResource(R.string.no_reservations))
            }

            Spacer(Modifier.height(16.dp))
            if (selectedRoute != null && selectedDate != null && selectedDeclaration != null) {
                Button(onClick = {
                    val decl = selectedDeclaration
                    if (decl != null) {
                        reservationViewModel.completeRoute(
                            context,
                            selectedRoute!!.id,
                            selectedDate!!,
                            decl.startTime,
                            decl
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
