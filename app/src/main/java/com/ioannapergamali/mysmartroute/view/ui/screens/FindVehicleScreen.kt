package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.utils.havePoiMembershipChanged
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AppDateTimeViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransferRequestViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Οθόνη εύρεσης οχήματος βάσει κόστους. Επαναχρησιμοποιεί την RouteModeScreen
 * ώστε να εμφανίζεται ο χάρτης, ο πίνακας με τα POI και το κουμπί "Εύρεση τώρα",
 * χωρίς επιλογή ημερομηνίας.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindVehicleScreen(navController: NavController, openDrawer: () -> Unit) {

    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val transferRequestViewModel: TransferRequestViewModel = viewModel()
    val dateViewModel: AppDateTimeViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()
    val appTime by dateViewModel.dateTime.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val routePoiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val routePois = routePoiIds.mapNotNull { id -> allPois.find { it.id == id } }
    val originalPoiIds = remember { mutableStateListOf<String>() }
    var editedRouteName by rememberSaveable { mutableStateOf("") }
    var startIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var endIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var maxCostText by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculating by remember { mutableStateOf(false) }
    var routeDuration by remember { mutableStateOf<Int?>(null) }
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    val hasPoiChanges by remember { derivedStateOf { havePoiMembershipChanged(originalPoiIds, routePoiIds) } }

    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    suspend fun saveEditedRouteIfChanged(): String {
        val routeId = selectedRouteId ?: return ""
        if (routePoiIds != originalPoiIds) {
            val newName = editedRouteName.trim().takeIf { it.isNotEmpty() }
            routeViewModel.updateRoute(context, routeId, routePoiIds, newName)
            originalPoiIds.clear()
            originalPoiIds.addAll(routePoiIds)
            routeViewModel.loadRoutes(context, includeAll = true)
        }
        return routeId
    }

    fun saveEditedRoute() {
        coroutineScope.launch {
            saveEditedRouteIfChanged()
            message = context.getString(R.string.route_saved)
        }
    }

    suspend fun resolveRouteForVehicleSearch(): Pair<String, Boolean> {
        val currentRouteId = selectedRouteId ?: return "" to false
        if (!hasPoiChanges) return currentRouteId to false

        val routeName = editedRouteName.trim()
        if (routeName.isEmpty()) {
            message = context.getString(R.string.route_name_required)
            return "" to false
        }
        val newRouteId = routeViewModel.addRoute(
            context,
            routePoiIds.toList(),
            routeName
        ) ?: return "" to false

        selectedRouteId = newRouteId
        originalPoiIds.clear()
        originalPoiIds.addAll(routePoiIds)
        editedRouteName = ""
        routeViewModel.loadRoutes(context, includeAll = true)

        return newRouteId to true
    }

    fun refreshRoute() {
        if (routePois.size >= 2) {
            coroutineScope.launch {
                calculating = true
                routeDuration = null
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
                routeDuration = data.duration.takeIf { it > 0 }
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
            routeDuration = null
        }
    }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
        dateViewModel.load(context)
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
            editedRouteName = ""
        }
    }

    LaunchedEffect(hasPoiChanges) {
        if (!hasPoiChanges) {
            editedRouteName = ""
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
                title = stringResource(R.string.find_vehicle),
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
                val selectedRoute = routes.find { it.id == selectedRouteId }
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
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
            }

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

            if (routePois.isNotEmpty()) {
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
                    value = editedRouteName,
                    onValueChange = { editedRouteName = it },
                    label = { Text(stringResource(R.string.route_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasPoiChanges
                )

                Spacer(Modifier.height(8.dp))

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

                OutlinedTextField(
                    value = routeDuration?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.duration)) },
                    placeholder = {
                        Text(
                            if (calculating) {
                                stringResource(R.string.calculating_route)
                            } else {
                                stringResource(R.string.duration_not_available)
                            }
                        )
                    },
                    trailingIcon = {
                        if (routeDuration != null) {
                            Text(stringResource(R.string.minutes_suffix))
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

            OutlinedTextField(
                value = maxCostText,
                onValueChange = { maxCostText = it },
                label = { Text(stringResource(R.string.cost)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val fromIdx = startIndex ?: return@launch
                            val toIdx = endIndex ?: return@launch
                            if (fromIdx >= toIdx) {
                                message = context.getString(R.string.invalid_stop_order)
                                return@launch
                            }
                            val fromId = routePois.getOrNull(fromIdx)?.id ?: return@launch
                            val toId = routePois.getOrNull(toIdx)?.id ?: return@launch
                            val cost = maxCostText.toDoubleOrNull()

                            val routeInfo = resolveRouteForVehicleSearch()
                            val routeId = routeInfo.first
                            if (routeId.isBlank()) {
                                if (message.isBlank()) {
                                    message = context.getString(R.string.request_unsuccessful)
                                }
                                return@launch
                            }

                            message = ""
                            navController.navigate(
                                "availableTransports?routeId=" +
                                    routeId +
                                    "&startId=" + fromId +
                                    "&endId=" + toId +
                                    "&maxCost=" + (cost?.toString() ?: "") +
                                    "&date=" +
                                    "&time="
                            )
                        }
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
                            val fromId = routePois.getOrNull(fromIdx)?.id ?: return@launch
                            val toId = routePois.getOrNull(toIdx)?.id ?: return@launch
                            val cost = maxCostText.toDoubleOrNull()

                            val (routeId, poiChanged) = resolveRouteForVehicleSearch()
                            if (routeId.isBlank()) {
                                if (message.isBlank()) {
                                    message = context.getString(R.string.request_unsuccessful)
                                }
                                return@launch
                            }

                            val requestDateTime = appTime ?: System.currentTimeMillis()
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
