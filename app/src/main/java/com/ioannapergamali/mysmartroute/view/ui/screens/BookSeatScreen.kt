package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ioannapergamali.mysmartroute.model.enumerations.BookingStep
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.BookingStepsIndicator
import com.ioannapergamali.mysmartroute.viewmodel.BookingViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.compose.ui.graphics.Color

private const val MARKER_ORANGE = BitmapDescriptorFactory.HUE_ORANGE
private const val MARKER_BLUE = BitmapDescriptorFactory.HUE_BLUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSeatScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: BookingViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val routes by viewModel.availableRoutes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRoute = routes.find { it.id == selectedRouteId }
    var message by remember { mutableStateOf("") }
    val poiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val userPoiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val pois = poiIds.mapNotNull { id -> allPois.find { it.id == id } }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculating by remember { mutableStateOf(false) }
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)
    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    val availableDates = declarations.filter { it.routeId == selectedRouteId }
        .sortedBy { it.date }
        .map { it.date to Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) }

    fun refreshRoute() {
        val currentPois = poiIds.mapNotNull { id -> allPois.find { it.id == id } }
        if (currentPois.size >= 2) {
            scope.launch {
                calculating = true
                val origin = LatLng(currentPois.first().lat, currentPois.first().lng)
                val destination = LatLng(currentPois.last().lat, currentPois.last().lng)
                val waypoints = currentPois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                val data = MapsUtils.fetchDurationAndPath(
                    origin,
                    destination,
                    apiKey,
                    VehicleType.CAR,
                    waypoints
                )
                pathPoints = data.points
                data.points.firstOrNull()?.let {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(it, 13f)
                    )
                }
                calculating = false
            }
        } else {
            pathPoints = emptyList()
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newName = savedStateHandle?.get<String>("poiName")
                val lat = savedStateHandle?.get<Double>("poiLat")
                val lng = savedStateHandle?.get<Double>("poiLng")
                if (newName != null && lat != null && lng != null) {
                    pendingPoi = Triple(newName, lat, lng)
                    poiViewModel.loadPois(context)
                }

                val newRoute = savedStateHandle?.get<String>("newRouteId")
                if (newRoute != null) {
                    savedStateHandle.remove<String>("newRouteId")
                    selectedRouteId = newRoute
                    viewModel.refreshRoutes()
                    routeViewModel.loadRoutes(context, includeAll = true)
                    scope.launch {
                        val (_, path) = routeViewModel.getRouteDirections(
                            context,
                            newRoute,
                            VehicleType.CAR
                        )
                        pathPoints = path
                        poiIds.clear()
                        userPoiIds.clear()
                        poiIds.addAll(routeViewModel.getRoutePois(context, newRoute).map { it.id })
                        path.firstOrNull()?.let {
                            MapsInitializer.initialize(context)
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngZoom(it, 13f)
                            )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allPois, pendingPoi) {
        pendingPoi?.let { (name, lat, lng) ->
            allPois.find { poi ->
                poi.name == name &&
                    abs(poi.lat - lat) < 0.00001 &&
                    abs(poi.lng - lng) < 0.00001
            }?.let { poi ->
                if (poiIds.none { it == poi.id }) {
                    savedStateHandle?.remove<String>("poiName")
                    savedStateHandle?.remove<Double>("poiLat")
                    savedStateHandle?.remove<Double>("poiLng")
                    poiIds.add(poi.id)
                    userPoiIds.add(poi.id)
                    refreshRoute()
                    pendingPoi = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
        declarationViewModel.loadDeclarations(context)
    }

    LaunchedEffect(selectedRouteId, poiIds, allPois) {
        if (selectedRouteId != null && poiIds.size >= 2 && pathPoints.isEmpty()) {
            refreshRoute()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.book_seat),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            var routeMenuExpanded by remember { mutableStateOf(false) }

            val currentStep = when {
                routes.isEmpty() -> BookingStep.DECLARE_ROUTE
                selectedRoute == null -> BookingStep.SELECT_ROUTE
                poiIds.size < 2 -> BookingStep.ADD_POI
                pathPoints.isEmpty() -> BookingStep.RECALCULATE_ROUTE
                datePickerState.selectedDateMillis == null -> BookingStep.SELECT_DATE
                else -> BookingStep.RESERVE_SEAT
            }

            BookingStepsIndicator(currentStep = currentStep)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("declareRoute") }) {
                Text(stringResource(R.string.declare_route))
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = routeMenuExpanded,
                onExpandedChange = { routeMenuExpanded = !routeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                selectedRouteId = route.id
                                routeMenuExpanded = false
                                scope.launch {
                                    val (_, path) = routeViewModel.getRouteDirections(
                                        context,
                                        route.id,
                                        VehicleType.CAR
                                    )
                                    pathPoints = path
                                    poiIds.clear()
                                    userPoiIds.clear()
                                    poiIds.addAll(routeViewModel.getRoutePois(context, route.id).map { it.id })
                                    path.firstOrNull()?.let {
                                        MapsInitializer.initialize(context)
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(it, 13f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedRoute != null) {
                Button(onClick = {
                    val rId = selectedRouteId ?: ""
                    navController.navigate("definePoi?lat=&lng=&source=&view=false&routeId=$rId")
                }) {
                    Text(stringResource(R.string.add_poi_option))
                }

                Spacer(Modifier.height(16.dp))
            }

            if (pois.size >= 2) {
                Button(onClick = { refreshRoute() }, enabled = !calculating) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.recalculate_route))
                }

                Spacer(Modifier.height(16.dp))
            }

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints, color = Color.Green)
                    pois.forEach { poi ->
                        val hue = if (poi.id in userPoiIds) MARKER_ORANGE else MARKER_BLUE
                        Marker(
                            state = MarkerState(position = LatLng(poi.lat, poi.lng)),
                            title = poi.name,
                            icon = BitmapDescriptorFactory.defaultMarker(hue)
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
                            Text("${index + 1}. ${poi.name}", modifier = Modifier.weight(1f))
                            Text(poi.type.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val removedId = poiIds.removeAt(index)
                                userPoiIds.remove(removedId)
                                refreshRoute()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_point))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            var dateMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dateMenuExpanded,
                onExpandedChange = { dateMenuExpanded = !dateMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = availableDates.isNotEmpty(),
                    label = { Text(stringResource(R.string.departure_date)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = dateMenuExpanded,
                    onDismissRequest = { dateMenuExpanded = false }
                ) {
                    availableDates.forEach { (millis, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                datePickerState.selectedDateMillis = millis
                                dateMenuExpanded = false
                            }
                        )
                    }
                }
            }
            if (availableDates.isEmpty()) {
                Text(stringResource(R.string.no_departures))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = selectedRoute != null,
                onClick = {
                    selectedRoute?.let { r ->
                        val success = viewModel.reserveSeat(r.id)
                        message = if (success) {
                            context.getString(R.string.seat_booked)
                        } else {
                            context.getString(R.string.seat_unavailable)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.reserve_seat))
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
