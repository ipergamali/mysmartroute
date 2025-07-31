package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routePois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromIndex by remember { mutableStateOf<Int?>(null) }
    var selectedToIndex by remember { mutableStateOf<Int?>(null) }
    var maxCostText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }
    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { id ->
            routePois = routeViewModel.getRoutePois(context, id)
            selectedFromIndex = null
            selectedToIndex = null
            val (_, path) = routeViewModel.getRouteDirections(
                context,
                id,
                VehicleType.CAR
            )
            pathPoints = path
            path.firstOrNull()?.let {
                MapsInitializer.initialize(context)
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(it, 13f)
                )
            }
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedStateHandle?.get<String>("newRouteId")?.let { newId ->
                    savedStateHandle.remove<String>("newRouteId")
                    selectedRouteId = newId
                    routeViewModel.loadRoutes(context, includeAll = true)
                    refreshRoute()
                }

                if (savedStateHandle?.contains("poiName") == true &&
                    savedStateHandle.contains("poiLat") &&
                    savedStateHandle.contains("poiLng") &&
                    selectedRouteId != null
                ) {
                    savedStateHandle.remove<String>("poiName")
                    savedStateHandle.remove<Double>("poiLat")
                    savedStateHandle.remove<Double>("poiLng")
                    routePois = routeViewModel.getRoutePois(context, selectedRouteId!!)
                    refreshRoute()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refreshRoute() {
        selectedRouteId?.let { id ->
            coroutineScope.launch {
                val (_, path) = routeViewModel.getRouteDirections(
                    context,
                    id,
                    VehicleType.CAR
                )
                pathPoints = path
                path.firstOrNull()?.let {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(it, 13f)
                    )
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

            if (routePois.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = !fromExpanded }) {
                    val startText = selectedFromIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: ""
                    OutlinedTextField(
                        value = startText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.boarding_stop)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        routePois.forEachIndexed { index, poi ->
                            DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                                selectedFromIndex = index
                                fromExpanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = !toExpanded }) {
                    val endText = selectedToIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: ""
                    OutlinedTextField(
                        value = endText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dropoff_stop)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                        routePois.forEachIndexed { index, poi ->
                            DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                                selectedToIndex = index
                                toExpanded = false
                            })
                        }
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
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("${'$'}{index + 1}. ${'$'}{poi.name}", modifier = Modifier.weight(1f))
                            Text(poi.type.name, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = maxCostText,
                onValueChange = { maxCostText = it },
                label = { Text(stringResource(R.string.max_cost)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (selectedRouteId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { navController.navigate("definePoi?routeId=${'$'}selectedRouteId") }) {
                        Text(stringResource(R.string.add_poi_option))
                    }
                    Button(onClick = { refreshRoute() }) {
                        Text(stringResource(R.string.recalculate_route))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val fromIdx = selectedFromIndex ?: return@Button
                    val toIdx = selectedToIndex ?: return@Button
                    if (fromIdx >= toIdx) {
                        message = context.getString(R.string.invalid_stop_order)
                        return@Button
                    }
                    val fromId = routePois[fromIdx].id
                    val toId = routePois[toIdx].id
                    val cost = maxCostText.toDoubleOrNull() ?: Double.MAX_VALUE
                    requestViewModel.requestTransport(context, fromId, toId, cost)
                    message = context.getString(R.string.request_sent)
                },
                enabled = selectedRouteId != null && selectedFromIndex != null && selectedToIndex != null
            ) {
                Text(stringResource(R.string.find_vehicle))
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
