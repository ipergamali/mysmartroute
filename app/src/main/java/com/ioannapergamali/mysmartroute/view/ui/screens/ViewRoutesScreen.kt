package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
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
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.offsetPois
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.FavoriteRoutesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRoutesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val favViewModel: FavoriteRoutesViewModel = viewModel()
    val favorites by favViewModel.favorites.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var newRouteName by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
        routeViewModel.loadRoutes(context, includeAll = true)
        favViewModel.loadFavorites(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_routes),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        },
        floatingActionButton = {
            if (selectedRoute != null) {
                FloatingActionButton(
                    onClick = {
                        selectedRoute?.let { route ->
                            if (!favorites.contains(route.id)) {
                                favViewModel.toggleFavorite(route.id)
                            }
                            favViewModel.saveFavorites(context) { success ->
                                val msg = if (success) {
                                    R.string.favorite_routes_saved
                                } else {
                                    R.string.favorite_routes_save_failed
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(msg),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }
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
                                    pois = routeViewModel.getRoutePois(context, route.id)

                                    pathPoints = pois.map { LatLng(it.lat, it.lng) }
                                    newRouteName = route.name
                                    pathPoints.firstOrNull()?.let {

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
                        scope.launch {
                            val ids = pois.map { it.id }
                            val result = routeViewModel.addRoute(context, ids, newRouteName)
                            val msg = if (result != null) {
                                routeViewModel.loadRoutes(context, includeAll = true)
                                R.string.route_saved
                            } else {
                                R.string.route_save_failed
                            }
                            Toast.makeText(
                                context,
                                context.getString(msg),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = newRouteName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_as_new_route))
                }
            }
        }
    }
}
