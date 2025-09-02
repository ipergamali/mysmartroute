package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
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
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(navController: NavController, openDrawer: () -> Unit) {
    val poiViewModel: PoIViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val availablePois by poiViewModel.pois.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        routeViewModel.loadRoutes(context, includeAll = true)
    }

    val routePoiIds = remember { mutableStateListOf<String>() }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routeMenuExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeName by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    val selectedRoute = routes.find { it.id == selectedRouteId }

    LaunchedEffect(selectedRouteId) {
        routeName = routes.find { it.id == selectedRouteId }?.name ?: ""
    }

    fun refreshRoute() {
        val pois = routePoiIds.mapNotNull { id -> availablePois.find { it.id == id } }
        if (pois.size >= 2 && !isKeyMissing) {
            scope.launch {
                val origin = LatLng(pois.first().lat, pois.first().lng)
                val destination = LatLng(pois.last().lat, pois.last().lng)
                val waypoints = pois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                val data = MapsUtils.fetchDurationAndPath(
                    origin,
                    destination,
                    apiKey,
                    VehicleType.CAR,
                    waypoints
                )
                pathPoints = data.points
                data.points.firstOrNull()?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
                }
            }
        } else {
            pathPoints = emptyList()
        }
    }

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.route_editor),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            ExposedDropdownMenuBox(
                expanded = routeMenuExpanded,
                onExpandedChange = { routeMenuExpanded = !routeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeMenuExpanded) },
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
                                routeName = route.name
                                scope.launch {
                                    val pois = routeViewModel.getRoutePois(context, route.id)
                                    routePoiIds.clear()
                                    routePoiIds.addAll(pois.map { it.id })
                                    refreshRoute()
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (isKeyMissing) {
                    Text(stringResource(R.string.map_api_key_missing))
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        if (pathPoints.isNotEmpty()) {
                            Polyline(points = pathPoints)
                        }
                        routePoiIds.mapNotNull { id -> availablePois.find { it.id == id } }
                            .forEach { poi ->
                                Marker(
                                    state = MarkerState(LatLng(poi.lat, poi.lng)),
                                    title = poi.name
                                )
                            }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            routePoiIds.forEachIndexed { index, id ->
                availablePois.find { it.id == id }?.let { poi ->
                    Text(text = "${index + 1}. ${poi.name}")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_stop))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (routePoiIds.isNotEmpty()) {
                            routePoiIds.removeLast()
                            refreshRoute()
                        }
                    },
                    enabled = routePoiIds.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_last_stop)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { refreshRoute() },
                    enabled = routePoiIds.size >= 2 && !isKeyMissing
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_route)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it },
                label = { Text(stringResource(R.string.route_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        selectedRouteId?.let { id ->
                            routeViewModel.updateRoute(context, id, routePoiIds, routeName)
                            routeViewModel.loadRoutes(context, includeAll = true)
                            Toast.makeText(
                                context,
                                context.getString(R.string.route_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = selectedRouteId != null && routePoiIds.size >= 2 && routeName.isNotBlank()
            ) {
                Text(stringResource(R.string.save_route))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                availablePois.forEach { poi ->
                    DropdownMenuItem(
                        text = { Text(poi.name) },
                        onClick = {
                            if (routePoiIds.lastOrNull() != poi.id) {
                                routePoiIds.add(poi.id)
                                refreshRoute()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.poi_already_last),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
