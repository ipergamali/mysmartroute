package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
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
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRoutesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_routes),
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
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                selectedRoute = route
                                expanded = false
                                scope.launch {
                                    val (_, path) = routeViewModel.getRouteDirections(
                                        context,
                                        route.id,
                                        VehicleType.CAR
                                    )
                                    pathPoints = path
                                    pois = routeViewModel.getRoutePois(context, route.id)
                                    path.firstOrNull()?.let {
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

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints)
                    pois.forEach { poi ->
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
                        }
                    }
                }
            }
        }
    }
}
