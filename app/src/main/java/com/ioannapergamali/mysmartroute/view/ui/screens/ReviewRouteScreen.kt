package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AdminRouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val adminViewModel: AdminRouteViewModel = viewModel(factory = AdminRouteViewModel.Factory(context))
    val routeViewModel: RouteViewModel = viewModel()
    val duplicateGroups by adminViewModel.duplicateRoutes.collectAsState()

    val routes = duplicateGroups.flatten()

    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var newRouteName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
    }

    LaunchedEffect(selectedRoute) {
        selectedRoute?.let { route ->
            pois = routeViewModel.getRoutePois(context, route.id)
            pathPoints = pois.map { LatLng(it.lat, it.lng) }
            pathPoints.firstOrNull()?.let {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.review_route),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (routes.isEmpty()) {
                Text(text = stringResource(R.string.no_duplicate_routes), modifier = Modifier.padding(16.dp))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(routes) { route ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    selectedRoute = route
                                    newRouteName = route.name
                                    pois = emptyList()
                                    pathPoints = emptyList()
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text(route.name.take(2))
                            }
                            Text(route.name)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedRoute != null && pois.isNotEmpty() && !isKeyMissing) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(id = R.dimen.map_height)),
                        cameraPositionState = cameraPositionState
                    ) {
                        if (pathPoints.size > 1) {
                            Polyline(points = pathPoints)
                        }
                        offsetPois(pois).forEach { (poi, position) ->
                            Marker(state = MarkerState(position = position), title = poi.name)
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

                Spacer(Modifier.height(16.dp))

                if (selectedRoute != null && pois.isNotEmpty()) {
                    OutlinedTextField(
                        value = newRouteName,
                        onValueChange = { newRouteName = it },
                        label = { Text(stringResource(R.string.new_route_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            selectedRoute?.let { route ->
                                scope.launch {
                                    val updated = route.copy(name = newRouteName)
                                    adminViewModel.updateRoute(updated)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.route_saved),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = newRouteName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
