package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareCompleteRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val reservationViewModel: ReservationViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val reservations by reservationViewModel.reservations.collectAsState()
    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }

    LaunchedEffect(selectedRoute) {
        selectedRoute?.let { route ->
            val (_, path) = routeViewModel.getRouteDirections(context, route.id, VehicleType.CAR)
            pathPoints = path
            pois = routeViewModel.getRoutePois(context, route.id)
            reservationViewModel.loadReservations(context, route.id)
            path.firstOrNull()?.let { cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f)) }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.prepare_complete_route),
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
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRoute = route
                            expanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints)
                    pois.forEach { poi ->
                        Marker(state = MarkerState(position = LatLng(poi.lat, poi.lng)), title = poi.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else if (isKeyMissing) {
                Text(stringResource(R.string.map_api_key_missing))
                Spacer(Modifier.height(16.dp))
            }

            if (reservations.isNotEmpty()) {
                Text(stringResource(R.string.print_list))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.driver), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.cost), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    Divider()
                    reservations.forEach { res ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(res.userId, modifier = Modifier.weight(1f))
                            Text("-", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
