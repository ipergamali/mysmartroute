package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.Polyline
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TransportAnnouncementViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MapSelectionMode { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController) {
    val viewModel: TransportAnnouncementViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var mapSelectionMode by remember { mutableStateOf<MapSelectionMode?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    var fromQuery by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var fromSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }

    var toQuery by remember { mutableStateOf("") }
    var toExpanded by remember { mutableStateOf(false) }
    var toSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }

    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }

    var startLatLng by remember { mutableStateOf<LatLng?>(null) }
    var endLatLng by remember { mutableStateOf<LatLng?>(null) }
    var costInput by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(0) }
    var dateInput by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.9838, 23.7275), 9f)
    }

    val apiKey = context.getString(R.string.google_maps_key)
    val isKeyMissing = apiKey.isBlank() || apiKey == "YOUR_API_KEY"

    LaunchedEffect(Unit) {
        vehicleViewModel.loadRegisteredVehicles(context)
    }

    LaunchedEffect(startLatLng, endLatLng, selectedVehicleType) {
        if (!isKeyMissing && startLatLng != null && endLatLng != null) {
            val type = selectedVehicleType ?: VehicleType.CAR
            val result = MapsUtils.fetchDurationAndPath(startLatLng!!, endLatLng!!, apiKey, type)
            val factor = when (selectedVehicleType) {
                VehicleType.BICYCLE -> 1.5
                VehicleType.MOTORBIKE -> 0.8
                VehicleType.BIGBUS -> 1.2
                VehicleType.SMALLBUS -> 1.1
                else -> 1.0
            }
            durationMinutes = (result.first * factor).toInt()
            routePoints = result.second
        }
    }

    LaunchedEffect(fromQuery) {
        if (fromQuery.length > 3) {
            fromSuggestions = withContext(Dispatchers.IO) {
                try { Geocoder(context).getFromLocationName(fromQuery, 5) ?: emptyList() } catch (e: Exception) { emptyList() }
            }
        } else {
            fromSuggestions = emptyList()
        }
    }

    LaunchedEffect(toQuery) {
        if (toQuery.length > 3) {
            toSuggestions = withContext(Dispatchers.IO) {
                try { Geocoder(context).getFromLocationName(toQuery, 5) ?: emptyList() } catch (e: Exception) { emptyList() }
            }
        } else {
            toSuggestions = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = "Announce Transport", navController = navController)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isKeyMissing) {
            GoogleMap(
                modifier = Modifier.weight(1f),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    when (mapSelectionMode) {
                        MapSelectionMode.FROM -> {
                            startLatLng = latLng
                            coroutineScope.launch {
                                val addr = withContext(Dispatchers.IO) {
                                    Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()
                                }
                                fromQuery = addr?.getAddressLine(0) ?: "${latLng.latitude},${latLng.longitude}"
                            }
                            mapSelectionMode = null
                        }
                        MapSelectionMode.TO -> {
                            endLatLng = latLng
                            coroutineScope.launch {
                                val addr = withContext(Dispatchers.IO) {
                                    Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()
                                }
                                toQuery = addr?.getAddressLine(0) ?: "${latLng.latitude},${latLng.longitude}"
                            }
                            mapSelectionMode = null
                        }
                        null -> {}
                    }
                }
            ) {
                startLatLng?.let {
                    Marker(state = rememberMarkerState(position = it), title = "From")
                }
                endLatLng?.let {
                    Marker(state = rememberMarkerState(position = it), title = "To")
                }
                if (routePoints.isNotEmpty()) {
                    Polyline(points = routePoints)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = !fromExpanded }) {
            TextField(
                value = fromQuery,
                onValueChange = { fromQuery = it; fromExpanded = true },
                label = { Text("From") },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val addr = withContext(Dispatchers.IO) {
                                    Geocoder(context).getFromLocationName(fromQuery, 1)?.firstOrNull()
                                }
                                addr?.let {
                                    startLatLng = LatLng(it.latitude, it.longitude)
                                    fromQuery = it.getAddressLine(0) ?: fromQuery
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(startLatLng!!, 10f)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Set From")
                        }
                        IconButton(onClick = { mapSelectionMode = MapSelectionMode.FROM }) {
                            Icon(Icons.Default.Place, contentDescription = "Pick From on Map")
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                fromSuggestions.forEach { address ->
                    DropdownMenuItem(
                        text = { Text(address.getAddressLine(0) ?: "") },
                        onClick = {
                            fromQuery = address.getAddressLine(0) ?: ""
                            startLatLng = LatLng(address.latitude, address.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(startLatLng!!, 10f)
                            fromExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = !toExpanded }) {
            TextField(
                value = toQuery,
                onValueChange = { toQuery = it; toExpanded = true },
                label = { Text("To") },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val addr = withContext(Dispatchers.IO) {
                                    Geocoder(context).getFromLocationName(toQuery, 1)?.firstOrNull()
                                }
                                addr?.let {
                                    endLatLng = LatLng(it.latitude, it.longitude)
                                    toQuery = it.getAddressLine(0) ?: toQuery
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(endLatLng!!, 10f)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Set To")
                        }
                        IconButton(onClick = { mapSelectionMode = MapSelectionMode.TO }) {
                            Icon(Icons.Default.Place, contentDescription = "Pick To on Map")
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                toSuggestions.forEach { address ->
                    DropdownMenuItem(
                        text = { Text(address.getAddressLine(0) ?: "") },
                        onClick = {
                            toQuery = address.getAddressLine(0) ?: ""
                            endLatLng = LatLng(address.latitude, address.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(endLatLng!!, 10f)
                            toExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var vehicleMenuExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(expanded = vehicleMenuExpanded, onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }) {
            TextField(
                value = selectedVehicleType?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Vehicle") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = vehicleMenuExpanded, onDismissRequest = { vehicleMenuExpanded = false }) {
                vehicles.forEach { entity ->
                    val type = try { VehicleType.valueOf(entity.type) } catch (e: Exception) { null }
                    type?.let {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = {
                                selectedVehicleType = it
                                vehicleMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = costInput, onValueChange = { costInput = it }, label = { Text("Cost") })
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Duration: $durationMinutes min")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("Date") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val cost = costInput.toDoubleOrNull() ?: 0.0
            val date = dateInput.toIntOrNull() ?: 0
            val start = startLatLng?.let { "${it.latitude},${it.longitude}" } ?: ""
            val end = endLatLng?.let { "${it.latitude},${it.longitude}" } ?: ""
            val route = Route(start, end, cost)
            val type = selectedVehicleType ?: VehicleType.CAR
            viewModel.announce(route, type, date, cost, durationMinutes)
        }) {
            Text("Announce")
        }

        if (state is TransportAnnouncementViewModel.AnnouncementState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = (state as TransportAnnouncementViewModel.AnnouncementState.Error).message)
        }
    }

    LaunchedEffect(state) {
        if (state is TransportAnnouncementViewModel.AnnouncementState.Success) {
            navController.popBackStack()
        }
    }
}
