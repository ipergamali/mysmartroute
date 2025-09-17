package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransferRequestViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.ATHENS_ZONE_ID
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.utils.combineDateAndTimeAsAthensInstant
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import kotlin.math.abs
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.utils.SessionManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteModeScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    @StringRes titleRes: Int = R.string.route_mode,
    includeCost: Boolean = false
) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val transferRequestViewModel: TransferRequestViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    // Store POI IDs so that the route can update dynamically
    val routePoiIds = remember { mutableStateListOf<String>() }
    // Derive the POI objects from the stored IDs.
    val routePois = routePoiIds.mapNotNull { id ->
        allPois.find { it.id == id }
    }
    val originalPoiIds = remember { mutableStateListOf<String>() }
    var startIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var endIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTimeMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(selectedTimeMillis) {
        selectedTimeMillis?.let {
            val totalMinutes = (it / 60000L).toInt()
            timePickerState.hour = (totalMinutes / 60) % 24
            timePickerState.minute = totalMinutes % 60
        }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ATHENS_ZONE_ID).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)
    val selectedTimeText = selectedTimeMillis?.let { millis ->
        LocalTime.ofSecondOfDay(millis / 1000).format(timeFormatter)
    } ?: stringResource(R.string.select_time)

    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculating by remember { mutableStateOf(false) }
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    var maxCostText by rememberSaveable { mutableStateOf("") }

    suspend fun saveEditedRouteIfChanged(): String {
        val routeId = selectedRouteId ?: return ""
        if (routePoiIds != originalPoiIds) {
            routeViewModel.updateRoute(context, routeId, routePoiIds)
            originalPoiIds.clear()
            originalPoiIds.addAll(routePoiIds)
        }
        return routeId
    }

    fun saveEditedRoute() {
        coroutineScope.launch {
            saveEditedRouteIfChanged()
            message = context.getString(R.string.route_saved)
        }
    }

    fun refreshRoute() {
        if (routePois.size >= 2) {
            coroutineScope.launch {
                calculating = true
                val origin = LatLng(routePois.first().lat, routePois.first().lng)
                val destination = LatLng(routePois.last().lat, routePois.last().lng)
                val waypoints = routePois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                val data = MapsUtils.fetchDurationAndPath(
                    origin,
                    destination,
                    apiKey,
                    VehicleType.CAR,
                    waypoints
                )
                pathPoints = data.points
                data.points.firstOrNull()?.let {
                    MapsInitializer.initialize(context)
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
    suspend fun resolveRouteForRequest(): Pair<String, Boolean> {
        val currentRouteId = selectedRouteId ?: return "" to false
        val currentCounts = routePoiIds.groupingBy { it }.eachCount()
        val originalCounts = originalPoiIds.groupingBy { it }.eachCount()
        if (currentCounts == originalCounts) return currentRouteId to false

        val uid = SessionManager.currentUserId() ?: return "" to false
        val username = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
            .getString("username") ?: uid
        val baseName = routes.find { it.id == currentRouteId }?.name ?: "route"
        val newRouteId = routeViewModel.addRoute(
            context,
            routePoiIds.toList(),
            "${baseName}_edited_by_$username"
        ) ?: return "" to false

        selectedRouteId = newRouteId
        originalPoiIds.clear()
        originalPoiIds.addAll(routePoiIds)
        routeViewModel.loadRoutes(context, includeAll = true)

        return newRouteId to true
    }


    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
    }

    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { id ->
            routePoiIds.clear()
            routePoiIds.addAll(routeViewModel.getRoutePois(context, id).map { it.id })
            originalPoiIds.clear()
            originalPoiIds.addAll(routePoiIds)
            startIndex = null
            endIndex = null
            refreshRoute()
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedStateHandle?.get<String>("poiName")?.let { newName ->
                    val lat = savedStateHandle.get<Double>("poiLat")
                    val lng = savedStateHandle.get<Double>("poiLng")
                    if (lat != null && lng != null) {
                        pendingPoi = Triple(newName, lat, lng)
                        poiViewModel.loadPois(context)
                    }
                }

                savedStateHandle?.get<String>("newRouteId")?.let { newId ->
                    savedStateHandle.remove<String>("newRouteId")
                    selectedRouteId = newId
                    routeViewModel.loadRoutes(context, includeAll = true)
                    refreshRoute()
                }

                poiViewModel.loadPois(context)
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
                if (routePoiIds.none { it == poi.id }) {
                    savedStateHandle?.remove<String>("poiName")
                    savedStateHandle?.remove<Double>("poiLat")
                    savedStateHandle?.remove<Double>("poiLng")
                    routePoiIds.add(poi.id)
                    refreshRoute()
                    pendingPoi = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(titleRes),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {

            Button(onClick = { navController.navigate("declareRoute") }) {
                Text(stringResource(R.string.declare_route))
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = !routeExpanded }) {
                OutlinedTextField(
                    value = routes.find { it.id == selectedRouteId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRouteId = route.id
                            routeExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedRouteId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val rId = selectedRouteId ?: ""
                        navController.navigate("definePoi?lat=&lng=&source=&view=false&routeId=$rId")
                    }) {
                        Text(stringResource(R.string.add_poi_option))
                    }
                    Button(onClick = { refreshRoute() }, enabled = !calculating) {
                        Text(stringResource(R.string.recalculate_route))
                    }
                    Button(onClick = { saveEditedRoute() }) {
                        Text(stringResource(R.string.save_route))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (routePois.isNotEmpty() && pathPoints.isNotEmpty() && !isKeyMissing) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        cameraPositionState = cameraPositionState
                    ) {
                        Polyline(points = pathPoints)
                        offsetPois(routePois).forEach { (poi, position) ->
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
            }

            if (routePois.isNotEmpty()) {
                Text(stringResource(R.string.stops_header))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.poi_name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.poi_type), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    Divider()
                    routePois.forEachIndexed { index, poi ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${poi.name}", modifier = Modifier.weight(1f))
                            Text(poi.type.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (endIndex == null || index < endIndex!!) {
                                    startIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83C\uDD95")
                            }
                            IconButton(onClick = {
                                if (startIndex == null || index > startIndex!!) {
                                    endIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83D\uDD1A")
                            }
                            IconButton(onClick = {
                                routePoiIds.removeAt(index)
                                if (startIndex != null) {
                                    if (index == startIndex) startIndex = null else if (index < startIndex!!) startIndex = startIndex!! - 1
                                }
                                if (endIndex != null) {
                                    if (index == endIndex) endIndex = null else if (index < endIndex!!) endIndex = endIndex!! - 1
                                }
                                refreshRoute()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_point))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = startIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.boarding_stop)) },
                    leadingIcon = { Text("\uD83C\uDD95") },
                    trailingIcon = {
                        IconButton(onClick = { startIndex = null }) {
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
                    value = endIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dropoff_stop)) },
                    leadingIcon = { Text("\uD83D\uDD1A") },
                    trailingIcon = {
                        IconButton(onClick = { endIndex = null }) {
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

                Spacer(Modifier.height(16.dp))
            }

            Button(onClick = { showDatePicker = true }) { Text(selectedDateText) }

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

            Button(onClick = { showTimePicker = true }) { Text(selectedTimeText) }

            if (showTimePicker) {
                TimePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                selectedTimeMillis =
                                    ((timePickerState.hour * 60) + timePickerState.minute) * 60_000L
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                ) {
                    TimePicker(state = timePickerState)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (includeCost) {
                OutlinedTextField(
                    value = maxCostText,
                    onValueChange = { maxCostText = it },
                    label = { Text(stringResource(R.string.cost)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val fromIdx = startIndex ?: return@Button
                        val toIdx = endIndex ?: return@Button
                        if (fromIdx >= toIdx) {
                            message = context.getString(R.string.invalid_stop_order)
                            return@Button
                        }
                        val fromId = routePois[fromIdx].id
                        val toId = routePois[toIdx].id
                        val cost = maxCostText.toDoubleOrNull()
                        val routeId = selectedRouteId ?: return@Button
                        val date = datePickerState.selectedDateMillis ?: 0L
                        val time = selectedTimeMillis

                        navController.navigate(
                            "availableTransports?routeId=" +
                                routeId +
                                "&startId=" + fromId +
                                "&endId=" + toId +
                                "&maxCost=" + (cost?.toString() ?: "") +
                                "&date=" + date +
                                "&time=" + (time?.toString() ?: "")
                        )
                    },
                    enabled = selectedRouteId != null && startIndex != null && endIndex != null,
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.find_now))
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val fromIdx = startIndex ?: return@launch
                            val toIdx = endIndex ?: return@launch
                            if (fromIdx >= toIdx) {
                                message = context.getString(R.string.invalid_stop_order)
                                return@launch
                            }
                            val fromId = routePois[fromIdx].id
                            val toId = routePois[toIdx].id
                            val cost = maxCostText.toDoubleOrNull()
                            val date = datePickerState.selectedDateMillis ?: 0L
                            val time = selectedTimeMillis ?: 0L

                            val (routeId, poiChanged) = resolveRouteForRequest()
                            if (routeId.isBlank()) {
                                message = context.getString(R.string.request_unsuccessful)
                                return@launch
                            }

                            val requestDateTime = combineDateAndTimeAsAthensInstant(date, time)
                            transferRequestViewModel.submitRequest(
                                context,
                                routeId,
                                fromId,
                                toId,
                                requestDateTime,
                                cost,
                                poiChanged
                            )
                            message = context.getString(R.string.request_sent)
                        }
                    },
                    enabled = selectedRouteId != null && startIndex != null && endIndex != null,
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_request))
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}

@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = content
    )
}

