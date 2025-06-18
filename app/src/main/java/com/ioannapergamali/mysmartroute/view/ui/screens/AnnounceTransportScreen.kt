package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import android.util.Log
import android.widget.Toast
import android.content.Context
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
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.BuildConfig
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.MapProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.CoordinateUtils
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TransportAnnouncementViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MapSelectionMode { FROM, TO }

private const val TAG = "AnnounceTransport"

private suspend fun reverseGeocode(context: Context, latLng: LatLng): String? =
    withContext(Dispatchers.IO) {
        try {
            Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }
    }

private suspend fun geocode(context: Context, query: String): Pair<LatLng, String>? =
    withContext(Dispatchers.IO) {
        try { Geocoder(context).getFromLocationName(query, 1)?.firstOrNull() } catch (e: Exception) { null }
    }?.let { Pair(LatLng(it.latitude, it.longitude), it.getAddressLine(0) ?: query) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: TransportAnnouncementViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val poiAddState by poiViewModel.addState.collectAsState()

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var mapSelectionMode by remember { mutableStateOf<MapSelectionMode?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var showRoute by remember { mutableStateOf(false) }

    var fromQuery by remember { mutableStateOf("") }
    var selectedFromDescription by remember { mutableStateOf<String?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var fromSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val fromPoiSuggestions = remember(fromQuery, pois) {
        pois.filter { it.name.contains(fromQuery, ignoreCase = true) }
    }

    var toQuery by remember { mutableStateOf("") }
    var selectedToDescription by remember { mutableStateOf<String?>(null) }
    var toExpanded by remember { mutableStateOf(false) }
    var toSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val toPoiSuggestions = remember(toQuery, pois) {
        pois.filter { it.name.contains(toQuery, ignoreCase = true) }
    }

    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }

    var startLatLng by remember { mutableStateOf<LatLng?>(null) }
    var endLatLng by remember { mutableStateOf<LatLng?>(null) }
    val fromMarkerState = rememberMarkerState()
    val toMarkerState = rememberMarkerState()
    var costInput by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(0) }
    var dateInput by remember { mutableStateOf("") }

    var fromError by remember { mutableStateOf(false) }
    var toError by remember { mutableStateOf(false) }
    var lastAddFrom by remember { mutableStateOf<Boolean?>(null) }

    // Αρχικοποίηση του χάρτη στο Ηράκλειο με ζουμ όπως στο ζητούμενο URL
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(35.3325932, 25.073835),
            13.79f
        )
    }

    val heraklionBounds = remember {
        LatLngBounds(
            LatLng(34.9, 24.8), // Southwest corner
            LatLng(35.5, 25.9)  // Northeast corner
        )
    }

    val mapProperties = remember { MapProperties(latLngBoundsForCameraTarget = heraklionBounds) }

    // Διαβάζουμε το API key μόνο από το BuildConfig
    val apiKey = BuildConfig.MAPS_API_KEY
    val isKeyMissing = apiKey.isBlank()
    Log.d(TAG, "API key loaded? ${!isKeyMissing}")

    LaunchedEffect(Unit) {
        vehicleViewModel.loadRegisteredVehicles(context)
        poiViewModel.loadPois(context)
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

    LaunchedEffect(startLatLng) {
        startLatLng?.let { fromMarkerState.position = it }
    }
    LaunchedEffect(endLatLng) {
        endLatLng?.let { toMarkerState.position = it }
    }

    ScreenContainer(modifier = Modifier.padding(0.dp)) {
        TopBar(
            title = "Announce Transport",
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isKeyMissing) {
            GoogleMap(
                modifier = Modifier.weight(1f),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                onMapLoaded = { Log.d(TAG, "Map loaded") },
                onMapClick = { latLng ->
                    when (mapSelectionMode) {
                        MapSelectionMode.FROM -> {
                            startLatLng = latLng
                            showRoute = false
                            coroutineScope.launch {
                                fromQuery = reverseGeocode(context, latLng) ?: "${latLng.latitude},${latLng.longitude}"
                                selectedFromDescription = fromQuery
                            }
                            fromError = false
                            mapSelectionMode = null
                        }
                        MapSelectionMode.TO -> {
                            endLatLng = latLng
                            showRoute = false
                            coroutineScope.launch {
                                toQuery = reverseGeocode(context, latLng) ?: "${latLng.latitude},${latLng.longitude}"
                                selectedToDescription = toQuery
                            }
                            toError = false
                            mapSelectionMode = null
                        }
                        null -> {}
                    }
                }
            ) {
                startLatLng?.let {
                    Marker(state = fromMarkerState, title = "From")
                }
                endLatLng?.let {
                    Marker(state = toMarkerState, title = "To")
                }
                if (showRoute && routePoints.isNotEmpty()) {
                    Polyline(points = routePoints)
                }
            }

            // Removed external maps button; map is shown directly on screen
        } else {
            Text(stringResource(R.string.map_api_key_missing))
        }

        if (startLatLng != null && endLatLng != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    Log.d(TAG, "Fetching directions from $startLatLng to $endLatLng")
                    Toast.makeText(context, "Αναζήτηση διαδρομής...", Toast.LENGTH_SHORT).show()
                    if (!isKeyMissing &&
                        CoordinateUtils.isValid(startLatLng) &&
                        CoordinateUtils.isValid(endLatLng)
                    ) {
                        if (NetworkUtils.isInternetAvailable(context)) {
                            val type = selectedVehicleType ?: VehicleType.CAR
                            val result = MapsUtils.fetchDurationAndPath(startLatLng!!, endLatLng!!, apiKey, type)
                            Log.d(TAG, "Directions API status: ${result.status}")
                            val factor = when (selectedVehicleType) {
                                VehicleType.BICYCLE -> 1.5
                                VehicleType.MOTORBIKE -> 0.8
                                VehicleType.BIGBUS -> 1.2
                                VehicleType.SMALLBUS -> 1.1
                                else -> 1.0
                            }
                            durationMinutes = (result.duration * factor).toInt()
                            routePoints = result.points
                            when {
                                result.status == "OK" && routePoints.isNotEmpty() -> {
                                    Log.d(TAG, "Route received with ${routePoints.size} points, duration $durationMinutes")
                                    Toast.makeText(context, "Διαδρομή βρέθηκε", Toast.LENGTH_SHORT).show()
                                }
                                result.status == "NOT_FOUND" || result.status == "INVALID_REQUEST" -> {
                                    Log.w(TAG, "Invalid coordinates provided")
                                    Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Log.w(TAG, "Route not found or API error: ${result.status}")
                                    Toast.makeText(context, "Δεν βρέθηκε διαδρομή", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
                    }
                    showRoute = true
                    Log.d(TAG, "Displaying route on map")
                }
            }) {
                Text(stringResource(R.string.directions))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (CoordinateUtils.isValid(startLatLng) && CoordinateUtils.isValid(endLatLng)) {
                Toast.makeText(context, context.getString(R.string.coordinates_valid), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.coordinates_missing), Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(stringResource(R.string.check_coordinates))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (NetworkUtils.isInternetAvailable(context)) {
                Toast.makeText(context, context.getString(R.string.internet_available), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(stringResource(R.string.check_internet))
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = !fromExpanded }) {
            TextField(
                value = fromQuery,
                onValueChange = {
                    fromQuery = it
                    fromExpanded = true
                    fromError = false
                    if (selectedFromDescription != null && fromQuery != selectedFromDescription) {
                        startLatLng = null
                        selectedFromDescription = null
                        showRoute = false
                    }
                },
                label = { Text("From") },
                isError = fromError,
                trailingIcon = {
                    Row {
                        if (fromError) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val result = geocode(context, fromQuery)
                                if (result != null) {
                                    startLatLng = result.first
                                    showRoute = false
                                    routePoints = emptyList()
                                    fromQuery = result.second
                                    selectedFromDescription = fromQuery
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(startLatLng!!, 10f)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Set From", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            startLatLng = null
                            fromQuery = ""
                            selectedFromDescription = null
                            routePoints = emptyList()
                            showRoute = false
                            mapSelectionMode = MapSelectionMode.FROM
                        }) {
                            Icon(Icons.Default.Place, contentDescription = "Pick From on Map", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            lastAddFrom = true
                            when {
                                startLatLng == null -> {
                                    Toast.makeText(context, "Επιλέξτε σημείο στον χάρτη", Toast.LENGTH_SHORT).show()
                                    fromError = true
                                }
                                fromQuery.isBlank() -> {
                                    Toast.makeText(context, "Η περιγραφή είναι κενή", Toast.LENGTH_SHORT).show()
                                    fromError = true
                                }
                                else -> {
                                    poiViewModel.addPoi(
                                        context,
                                        fromQuery,
                                        fromQuery,
                                        "HISTORICAL",
                                        startLatLng!!.latitude,
                                        startLatLng!!.longitude
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Save From POI", tint = MaterialTheme.colorScheme.primary)
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                fromSuggestions.forEach { address ->
                    DropdownMenuItem(
                        text = { Text(address.getAddressLine(0) ?: "") },
                        onClick = {
                            fromQuery = address.getAddressLine(0) ?: ""
                            startLatLng = LatLng(address.latitude, address.longitude)
                            selectedFromDescription = fromQuery
                            showRoute = false
                            routePoints = emptyList()
                            fromError = false
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(startLatLng!!, 10f)
                            fromExpanded = false
                        }
                    )
                }
                fromPoiSuggestions.forEach { poi ->
                    DropdownMenuItem(
                        text = { Text(poi.name) },
                        onClick = {
                            fromQuery = poi.name
                            startLatLng = LatLng(poi.lat, poi.lng)
                            selectedFromDescription = fromQuery
                            showRoute = false
                            routePoints = emptyList()
                            fromError = false
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
                onValueChange = {
                    toQuery = it
                    toExpanded = true
                    toError = false
                    if (selectedToDescription != null && toQuery != selectedToDescription) {
                        endLatLng = null
                        selectedToDescription = null
                        showRoute = false
                    }
                },
                label = { Text("To") },
                isError = toError,
                trailingIcon = {
                    Row {
                        if (toError) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val result = geocode(context, toQuery)
                                if (result != null) {
                                    endLatLng = result.first
                                    showRoute = false
                                    routePoints = emptyList()
                                    toQuery = result.second
                                    selectedToDescription = toQuery
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(endLatLng!!, 10f)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Set To", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            endLatLng = null
                            toQuery = ""
                            selectedToDescription = null
                            routePoints = emptyList()
                            showRoute = false
                            mapSelectionMode = MapSelectionMode.TO
                        }) {
                            Icon(Icons.Default.Place, contentDescription = "Pick To on Map", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            lastAddFrom = false
                            when {
                                endLatLng == null -> {
                                    Toast.makeText(context, "Επιλέξτε σημείο στον χάρτη", Toast.LENGTH_SHORT).show()
                                    toError = true
                                }
                                toQuery.isBlank() -> {
                                    Toast.makeText(context, "Η περιγραφή είναι κενή", Toast.LENGTH_SHORT).show()
                                    toError = true
                                }
                                else -> {
                                    poiViewModel.addPoi(
                                        context,
                                        toQuery,
                                        toQuery,
                                        "HISTORICAL",
                                        endLatLng!!.latitude,
                                        endLatLng!!.longitude
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Save To POI", tint = MaterialTheme.colorScheme.primary)
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                toSuggestions.forEach { address ->
                    DropdownMenuItem(
                        text = { Text(address.getAddressLine(0) ?: "") },
                        onClick = {
                            toQuery = address.getAddressLine(0) ?: ""
                            endLatLng = LatLng(address.latitude, address.longitude)
                            selectedToDescription = toQuery
                            showRoute = false
                            routePoints = emptyList()
                            toError = false
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(endLatLng!!, 10f)
                            toExpanded = false
                        }
                    )
                }
                toPoiSuggestions.forEach { poi ->
                    DropdownMenuItem(
                        text = { Text(poi.name) },
                        onClick = {
                            toQuery = poi.name
                            endLatLng = LatLng(poi.lat, poi.lng)
                            selectedToDescription = toQuery
                            showRoute = false
                            routePoints = emptyList()
                            toError = false
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
            DropdownMenu(expanded = vehicleMenuExpanded, onDismissRequest = { vehicleMenuExpanded = false }) {
                vehicles.forEach { entity ->
                    val type = try { VehicleType.valueOf(entity.type) } catch (e: Exception) { null }
                    type?.let {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = {
                                selectedVehicleType = it
                                showRoute = false
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
            if (fromQuery.isBlank() || startLatLng == null || toQuery.isBlank() || endLatLng == null) {
                Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
            } else {
                val cost = costInput.toDoubleOrNull() ?: 0.0
                val date = dateInput.toIntOrNull() ?: 0
                val start = "${startLatLng!!.latitude},${startLatLng!!.longitude}"
                val end = "${endLatLng!!.latitude},${endLatLng!!.longitude}"
                val route = Route(start, end, cost)
                val type = selectedVehicleType ?: VehicleType.CAR
                viewModel.announce(route, type, date, cost, durationMinutes)
            }
        }) {
            Text("Announce")
        }

        if (state is TransportAnnouncementViewModel.AnnouncementState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = (state as TransportAnnouncementViewModel.AnnouncementState.Error).message)
    }

    LaunchedEffect(state) {
        if (state is TransportAnnouncementViewModel.AnnouncementState.Success) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(poiAddState) {
        when (poiAddState) {
            PoIViewModel.AddPoiState.Success -> {
                Toast.makeText(context, "POI αποθηκεύτηκε", Toast.LENGTH_SHORT).show()
                fromError = false
                toError = false
                poiViewModel.resetAddState()
            }
            PoIViewModel.AddPoiState.Exists -> {
                Toast.makeText(context, "Το POI είναι ήδη καταχωρημένο", Toast.LENGTH_SHORT).show()
                if (lastAddFrom == true) {
                    fromError = true
                } else if (lastAddFrom == false) {
                    toError = true
                }
                poiViewModel.resetAddState()
            }
            else -> {}
        }
    }
    }
}
