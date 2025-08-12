@file:OptIn(ExperimentalMaterial3Api::class)
package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.abs

import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import android.app.TimePickerDialog
import java.util.Calendar

@Composable
fun WalkingScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val routePoiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val routePois = routePoiIds.mapNotNull { id -> allPois.find { it.id == id } }
    var startIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var endIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var timeText by rememberSaveable { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculating by remember { mutableStateOf(false) }
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                timeText = "%02d:%02d".format(hour, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
        showTimePicker = false
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


    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
    }
    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { id ->
            routePoiIds.clear()
            routePoiIds.addAll(routeViewModel.getRoutePois(context, id).map { it.id })
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
                title = stringResource(R.string.walking),
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
                    routePois.forEach { poi ->
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
                                }
                            }) {
                                Text("\uD83C\uDD95")
                            }
                            IconButton(onClick = {
                                if (startIndex == null || index > startIndex!!) {
                                    endIndex = index
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

            OutlinedTextField(
                value = timeText,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.time)) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.time))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    } }
