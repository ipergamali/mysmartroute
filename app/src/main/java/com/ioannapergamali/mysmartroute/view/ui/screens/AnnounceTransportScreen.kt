package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.CoordinateUtils
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.viewmodel.TransportAnnouncementViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.google.android.libraries.places.api.model.Place
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
    var routePoints by rememberSaveable { mutableStateOf<List<LatLng>>(emptyList()) }
    var showRoute by rememberSaveable { mutableStateOf(false) }
    val routePois = remember { mutableStateListOf<PoIEntity>() }
    var stopMenuExpanded by remember { mutableStateOf(false) }

    var fromQuery by rememberSaveable { mutableStateOf("") }
    var selectedFromDescription by rememberSaveable { mutableStateOf<String?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var fromSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val fromFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val fromPoiSuggestions = remember(fromQuery, pois) {
        if (fromQuery.isBlank()) emptyList() else
        pois.filter { it.name.contains(fromQuery, ignoreCase = true) }
            .sortedBy { it.name }
    }

    var toQuery by rememberSaveable { mutableStateOf("") }
    var selectedToDescription by rememberSaveable { mutableStateOf<String?>(null) }
    var toExpanded by remember { mutableStateOf(false) }
    var toSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val toFocusRequester = remember { FocusRequester() }
    val toPoiSuggestions = remember(toQuery, pois) {
        if (toQuery.isBlank()) emptyList() else
        pois.filter { it.name.contains(toQuery, ignoreCase = true) }
            .sortedBy { it.name }
    }

    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }

    var startLatLng by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var endLatLng by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var fromSelectedIsPoi by rememberSaveable { mutableStateOf(false) }
    var toSelectedIsPoi by rememberSaveable { mutableStateOf(false) }
    var fromConfirmed by rememberSaveable { mutableStateOf(false) }
    var toConfirmed by rememberSaveable { mutableStateOf(false) }
    val fromMarkerState = rememberMarkerState()
    val toMarkerState = rememberMarkerState()
    var costInput by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(0) }
    var dateInput by remember { mutableStateOf("") }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val savedHandle = backStackEntry?.savedStateHandle
    val resultPoiName by savedHandle?.getStateFlow<String?>("poiName", null)?.collectAsState(initial = null) ?: remember { mutableStateOf<String?>(null) }
    val resultLat by savedHandle?.getStateFlow<Double?>("poiLat", null)?.collectAsState(initial = null) ?: remember { mutableStateOf<Double?>(null) }
    val resultLng by savedHandle?.getStateFlow<Double?>("poiLng", null)?.collectAsState(initial = null) ?: remember { mutableStateOf<Double?>(null) }
    val resultFrom by savedHandle?.getStateFlow<Boolean?>("poiFrom", null)?.collectAsState(initial = null) ?: remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(resultPoiName, resultLat, resultLng, resultFrom) {
        if (resultPoiName != null && resultLat != null && resultLng != null && resultFrom != null) {
            val latLng = LatLng(resultLat!!, resultLng!!)
            if (resultFrom == true) {
                startLatLng = latLng
                fromQuery = resultPoiName!!
                selectedFromDescription = resultPoiName
                fromSelectedIsPoi = true
            } else {
                endLatLng = latLng
                toQuery = resultPoiName!!
                selectedToDescription = resultPoiName
                toSelectedIsPoi = true
            }
            savedHandle?.remove<String>("poiName")
            savedHandle?.remove<Double>("poiLat")
            savedHandle?.remove<Double>("poiLng")
            savedHandle?.remove<Boolean>("poiFrom")
        }
    }

    var fromError by remember { mutableStateOf(false) }
    var toError by remember { mutableStateOf(false) }
    var poiTypeExpanded by remember { mutableStateOf(false) }
    // Χρησιμοποιούμε το ESTABLISHMENT ως προεπιλεγμένο είδος σημείου
    var selectedPoiType by remember { mutableStateOf<Place.Type>(Place.Type.ESTABLISHMENT) }

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

    // Ιδιότητες χάρτη με περιορισμό στα όρια του Ηρακλείου
    val mapProperties = remember {
        MapProperties(latLngBoundsForCameraTarget = heraklionBounds)
    }

    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    Log.d(TAG, "API key loaded? ${!isKeyMissing}")

    val fetchRoute: () -> Unit = {
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
    }

    LaunchedEffect(Unit) {
        vehicleViewModel.loadRegisteredVehicles(context)
        poiViewModel.loadPois(context)
    }


    LaunchedEffect(fromQuery) {
        if (fromQuery.length > 3) {
            fromSuggestions = withContext(Dispatchers.IO) {
                try {
                    Geocoder(context).getFromLocationName(fromQuery, 5) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }.sortedBy { it.getAddressLine(0) ?: "" }
        } else {
            fromSuggestions = emptyList()
        }
    }

    LaunchedEffect(fromExpanded) {
        if (fromExpanded) {
            fromFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(startLatLng, endLatLng) {
        if (!isKeyMissing && startLatLng != null && endLatLng != null) {
            durationMinutes = MapsUtils.fetchDuration(
                startLatLng!!,
                endLatLng!!,
                apiKey,
                selectedVehicleType ?: VehicleType.CAR
            )
        }
    }

    LaunchedEffect(toQuery) {
        if (toQuery.length > 3) {
            toSuggestions = withContext(Dispatchers.IO) {
                try { Geocoder(context).getFromLocationName(toQuery, 5) ?: emptyList() } catch (e: Exception) { emptyList() }
            }.sortedBy { it.getAddressLine(0) ?: "" }
        } else {
            toSuggestions = emptyList()
        }
    }

    LaunchedEffect(toExpanded) {
        if (toExpanded) {
            toFocusRequester.requestFocus()
            keyboardController?.show()
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
            title = stringResource(R.string.announce_transport),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isKeyMissing) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
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
                            navController.navigate("definePoi?lat=${latLng.latitude}&lng=${latLng.longitude}&source=from&view=false")
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
                            navController.navigate("definePoi?lat=${latLng.latitude}&lng=${latLng.longitude}&source=to&view=false")
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
                routePois.forEachIndexed { index, poi ->
                    Marker(
                        state = rememberMarkerState(position = LatLng(poi.lat, poi.lng)),
                        title = "${index + 1}. ${poi.name}"
                    )
                }
                if (showRoute && routePoints.isNotEmpty()) {
                    Polyline(points = routePoints)
                }
            }

            // Removed external maps button; map is shown directly on screen
        } else {
            Text(
                stringResource(R.string.map_api_key_missing),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = fromExpanded,
                onExpandedChange = {
                    if (fromQuery.isNotBlank()) {
                        fromExpanded = !fromExpanded
                        if (fromExpanded) fromFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
            OutlinedTextField(
                value = fromQuery,
                onValueChange = {
                    fromQuery = it
                    fromExpanded = it.isNotBlank()
                    fromError = false
                    fromConfirmed = false
                    if (selectedFromDescription != null && fromQuery != selectedFromDescription) {
                        startLatLng = null
                        selectedFromDescription = null
                        showRoute = false
                    }
                },
                label = { Text(stringResource(R.string.start_point)) },
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
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                    .focusRequester(fromFocusRequester),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            DropdownMenu(
                expanded = fromExpanded,
                onDismissRequest = { fromExpanded = false },
                modifier = Modifier.heightIn(max = 200.dp),
                properties = PopupProperties(focusable = false)
            ) {
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
                            fromSelectedIsPoi = false
                            navController.navigate("definePoi?lat=${startLatLng!!.latitude}&lng=${startLatLng!!.longitude}&source=from&view=false")
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
                            fromSelectedIsPoi = true
                        }
                    )
                }
            }
        }
        if (startLatLng != null && selectedFromDescription != null) {
            IconButton(onClick = {
                fromConfirmed = true
                if (fromConfirmed && toConfirmed) fetchRoute()
            }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (startLatLng != null && fromSelectedIsPoi) {
            IconButton(onClick = {
                navController.navigate("definePoi?lat=${startLatLng!!.latitude}&lng=${startLatLng!!.longitude}&source=from&view=true")
            }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.poi_details),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(
            onClick = {
                startLatLng = null
                fromQuery = ""
                selectedFromDescription = null
                routePoints = emptyList()
                showRoute = false
                mapSelectionMode = MapSelectionMode.FROM
                },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Pick From on Map",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (startLatLng != null && endLatLng != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                fetchRoute()
            }) {
                Text(stringResource(R.string.directions))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box {
            Button(onClick = { stopMenuExpanded = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_stop))
            }
            DropdownMenu(expanded = stopMenuExpanded, onDismissRequest = { stopMenuExpanded = false }) {
                pois.forEach { poi ->
                    DropdownMenuItem(
                        text = { Text(poi.name) },
                        onClick = {
                            routePois.add(poi)
                            stopMenuExpanded = false
                        }
                    )
                }
            }
        }
        routePois.forEachIndexed { index, poi ->
            Text(text = "${index + 1}. ${poi.name}")
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


        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = toExpanded,
                onExpandedChange = {
                    if (toQuery.isNotBlank()) {
                        toExpanded = !toExpanded
                        if (toExpanded) toFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
            OutlinedTextField(
                value = toQuery,
                onValueChange = {
                    toQuery = it
                    toExpanded = it.isNotBlank()
                    toError = false
                    toConfirmed = false
                    if (selectedToDescription != null && toQuery != selectedToDescription) {
                        endLatLng = null
                        selectedToDescription = null
                        showRoute = false
                    }
                },
                label = { Text(stringResource(R.string.to)) },
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
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                    .focusRequester(toFocusRequester),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            DropdownMenu(
                expanded = toExpanded,
                onDismissRequest = { toExpanded = false },
                modifier = Modifier.heightIn(max = 200.dp),
                properties = PopupProperties(focusable = false)
            ) {
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
                            toSelectedIsPoi = false
                            navController.navigate("definePoi?lat=${endLatLng!!.latitude}&lng=${endLatLng!!.longitude}&source=to&view=false")
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
                            toSelectedIsPoi = true
                        }
                    )
                }
            }
        }
        if (endLatLng != null && selectedToDescription != null) {
            IconButton(onClick = {
                toConfirmed = true
                if (fromConfirmed && toConfirmed) fetchRoute()
            }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (endLatLng != null && toSelectedIsPoi) {
            IconButton(onClick = {
                navController.navigate("definePoi?lat=${endLatLng!!.latitude}&lng=${endLatLng!!.longitude}&source=to&view=true")
            }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.poi_details),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(
            onClick = {
                endLatLng = null
                toQuery = ""
                selectedToDescription = null
                routePoints = emptyList()
                showRoute = false
                mapSelectionMode = MapSelectionMode.TO
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Pick To on Map",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = poiTypeExpanded, onExpandedChange = { poiTypeExpanded = !poiTypeExpanded }) {
            OutlinedTextField(
                value = selectedPoiType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.poi_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = poiTypeExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            DropdownMenu(expanded = poiTypeExpanded, onDismissRequest = { poiTypeExpanded = false }) {
                Place.Type.values().forEach { t ->
                    DropdownMenuItem(text = { Text(t.name) }, onClick = {
                        selectedPoiType = t
                        poiTypeExpanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var vehicleMenuExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(expanded = vehicleMenuExpanded, onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }) {
            OutlinedTextField(
                value = selectedVehicleType?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.vehicle)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
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
        OutlinedTextField(
            value = costInput,
            onValueChange = { costInput = it },
            label = { Text(stringResource(R.string.cost)) },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.duration_format, durationMinutes))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateInput,
            onValueChange = { dateInput = it },
            label = { Text(stringResource(R.string.date)) },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (fromQuery.isBlank() || startLatLng == null || toQuery.isBlank() || endLatLng == null) {
                Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
            } else {
                val cost = costInput.toDoubleOrNull() ?: 0.0
                val date = dateInput.toIntOrNull() ?: 0
                val start = "${startLatLng!!.latitude},${startLatLng!!.longitude}"
                val end = "${endLatLng!!.latitude},${endLatLng!!.longitude}"
                val route = Route(start, end, cost, routePois.toMutableList())
                val type = selectedVehicleType ?: VehicleType.CAR
                viewModel.announce(context, route, type, date, cost, durationMinutes)
            }
        }) {
            Text(stringResource(R.string.announce))
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
                poiViewModel.resetAddState()
            }
            else -> {}
        }
    }
    }
}
}
