package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
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
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AdminRouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val adminViewModel: AdminRouteViewModel = viewModel(factory = AdminRouteViewModel.Factory(context))
    val duplicateGroups by adminViewModel.duplicateRoutes.collectAsState()
    val routeViewModel: RouteViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var editedName by remember { mutableStateOf("") }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) { MapsInitializer.initialize(context) }

    Scaffold(topBar = {
        TopBar(title = stringResource(R.string.review_route), navController = navController, showMenu = true, onMenuClick = openDrawer)
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (duplicateGroups.isEmpty()) {
                Text(text = stringResource(R.string.no_duplicate_routes), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(duplicateGroups) { group ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            group.forEach { route ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    RadioButton(
                                        selected = selectedRoute?.id == route.id,
                                        onClick = {
                                            selectedRoute = route
                                            editedName = route.name
                                            scope.launch {
                                                val (_, path) = routeViewModel.getRouteDirections(context, route.id, VehicleType.CAR)
                                                pathPoints = path
                                                pois = routeViewModel.getRoutePois(context, route.id)
                                                path.firstOrNull()?.let {
                                                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
                                                }
                                            }
                                        }
                                    )
                                    Text(text = route.name)
                                }
                            }
                            Divider()
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
                        offsetPois(pois).forEach { (poi, position) ->
                            Marker(state = MarkerState(position = position), title = poi.name)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (selectedRoute != null && isKeyMissing) {
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
                    Spacer(Modifier.height(16.dp))
                }

                if (selectedRoute != null) {
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(stringResource(R.string.new_route_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        selectedRoute?.let {
                            scope.launch {
                                routeViewModel.addRoute(
                                    context,
                                    pois.map { p -> p.id },
                                    editedName
                                )
                            }
                        }
                        selectedRoute = null
                        editedName = ""
                        pois = emptyList()
                        pathPoints = emptyList()
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}
