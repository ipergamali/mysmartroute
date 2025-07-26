package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareCompleteRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val reservationViewModel: ReservationViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val reservations by reservationViewModel.reservations.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        declarationViewModel.loadDeclarations(context)
        poiViewModel.loadPois(context)
    }

    LaunchedEffect(selectedRoute, selectedDate) {
        val date = selectedDate
        selectedRoute?.let { route ->
            val (_, path) = routeViewModel.getRouteDirections(context, route.id, VehicleType.CAR)
            pathPoints = path
            pois = routeViewModel.getRoutePois(context, route.id)
            if (date != null) {
                reservationViewModel.loadReservations(context, route.id, date)
            }
            path.firstOrNull()?.let {
                MapsInitializer.initialize(context)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
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
            Box {
                Button(onClick = { expanded = true }) {
                    Text(selectedRoute?.name ?: stringResource(R.string.select_route))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRoute = route
                            expanded = false
                            selectedDate = null
                        })
                    }
                }
            }

            val availableDates = declarations.filter { it.routeId == selectedRoute?.id }
                .sortedBy { it.date }
                .map { it.date }

            if (selectedRoute != null) {
                var dateMenuExpanded by remember { mutableStateOf(false) }
                val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy") }

                ExposedDropdownMenuBox(expanded = dateMenuExpanded, onExpandedChange = { dateMenuExpanded = !dateMenuExpanded }) {
                    val text = selectedDate?.let { millis ->
                        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)
                    } ?: stringResource(R.string.select_date)
                    OutlinedTextField(
                        value = text,
                        onValueChange = {},
                        readOnly = true,
                        enabled = availableDates.isNotEmpty(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text(stringResource(R.string.departure_date)) }
                    )
                    ExposedDropdownMenu(expanded = dateMenuExpanded, onDismissRequest = { dateMenuExpanded = false }) {
                        availableDates.forEach { millis ->
                            val textDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)
                            DropdownMenuItem(text = { Text(textDate) }, onClick = {
                                selectedDate = millis
                                dateMenuExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            val seats = declarations.firstOrNull { it.routeId == selectedRoute?.id && it.date == selectedDate }?.seats ?: 0

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                        val userName by produceState(initialValue = res.userId, key1 = res.userId) {
                            val name = userViewModel.getUserName(context, res.userId)
                            value = if (name.isNotBlank()) name else res.userId
                        }
                        val startName by produceState(initialValue = "-", key1 = res.startPoiId) {
                            val name = poiViewModel.getPoiName(context, res.startPoiId)
                            value = if (name.isNotBlank()) name else "-"
                        }
                        val endName by produceState(initialValue = "-", key1 = res.endPoiId) {
                            val name = poiViewModel.getPoiName(context, res.endPoiId)
                            value = if (name.isNotBlank()) name else "-"
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(userName, modifier = Modifier.weight(1f))
                            Text(startName, modifier = Modifier.weight(1f))
                            Text(endName, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
