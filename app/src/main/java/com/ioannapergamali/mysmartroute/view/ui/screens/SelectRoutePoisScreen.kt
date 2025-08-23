package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRoutePoisScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var pois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val selectedPoiIds = remember { mutableStateListOf<String>() }
    val originalPoiIds = remember { mutableStateListOf<String>() }

    val cameraPositionState = rememberCameraPositionState()
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    suspend fun saveEditedRouteIfChanged(): Boolean? {
        if (selectedPoiIds.toList() == originalPoiIds.toList()) return null
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val username = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
            .getString("username") ?: uid
        val baseName = selectedRoute?.name ?: "route"
        return routeViewModel.addRoute(
            context,
            selectedPoiIds.toList(),
            "${baseName}_edited_by_$username"
        ) != null
    }

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.select_pois_screen_title),
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
                                    selectedPoiIds.clear()
                                    selectedPoiIds.addAll(pois.map { it.id })
                                    originalPoiIds.clear()
                                    originalPoiIds.addAll(selectedPoiIds)
                                    path.firstOrNull()?.let {
                                        MapsInitializer.initialize(context)
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
                Text(stringResource(R.string.choose_pois))
                Column(modifier = Modifier.fillMaxWidth()) {
                    pois.forEachIndexed { index, poi ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = poi.id in selectedPoiIds,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (poi.id !in selectedPoiIds) selectedPoiIds.add(poi.id)
                                    } else {
                                        selectedPoiIds.remove(poi.id)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${index + 1}. ${poi.name}")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(onClick = {
                    scope.launch {
                        val saved = saveEditedRouteIfChanged()
                        if (saved == true) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.route_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.confirm_poi_selection))
                }
            }
        }
    }
}

