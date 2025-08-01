  package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import android.widget.Toast
import android.location.Geocoder
import android.location.Address
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.foundation.clickable
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Polyline
import com.ioannapergamali.mysmartroute.utils.nullableLatLngSaver
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.navigation.NavController
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.launch
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val TAG = "DeclareRoute"
private const val MARKER_RED = BitmapDescriptorFactory.HUE_RED
private const val MARKER_GREEN = BitmapDescriptorFactory.HUE_GREEN
private const val MARKER_BLUE = BitmapDescriptorFactory.HUE_BLUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclareRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context: Context = LocalContext.current
    val poiViewModel: PoIViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val pois by poiViewModel.pois.collectAsState()

    LaunchedEffect(Unit) { poiViewModel.loadPois(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(35.3325932, 25.073835),
            13.79f
        )
    }

    // Όρια κάμερας ώστε ο χάρτης να περιορίζεται στην πόλη του Ηρακλείου
    val heraklionBounds = LatLngBounds(
        LatLng(35.28, 25.05), // νοτιοδυτικό όριο
        LatLng(35.40, 25.20)  // βορειοανατολικό όριο
    )
    val mapProperties = MapProperties(latLngBoundsForCameraTarget = heraklionBounds)

    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    Log.d(TAG, "API key loaded? ${!isKeyMissing}")

    val routePois by routeViewModel.currentRoute.collectAsState()
    val pathPoints = remember { mutableStateListOf<LatLng>() }
    var routeName by rememberSaveable { mutableStateOf("") }
    var routeSaved by remember { mutableStateOf(false) }
    var calculating by remember { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPoiId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPoi = selectedPoiId?.let { id -> pois.find { it.id == id } }
    var selectingPoint by rememberSaveable { mutableStateOf(false) }
    var unsavedPoint: LatLng? by rememberSaveable(stateSaver = nullableLatLngSaver()) {
        mutableStateOf<LatLng?>(null)
    }
    var unsavedAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var addressQuery by remember { mutableStateOf("") }
    var addressResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var addressMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    var removeIndex by remember { mutableStateOf<Int?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun goToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val target = LatLng(it.latitude, it.longitude)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(target, 15f))
            }
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newName = savedStateHandle?.get<String>("poiName")
                val lat = savedStateHandle?.get<Double>("poiLat")
                val lng = savedStateHandle?.get<Double>("poiLng")
                if (newName != null && lat != null && lng != null) {
                    pendingPoi = Triple(newName, lat, lng)
                    poiViewModel.loadPois(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pois, pendingPoi) {
        pendingPoi?.let { (name, lat, lng) ->
            savedStateHandle?.remove<String>("poiName")
            savedStateHandle?.remove<Double>("poiLat")
            savedStateHandle?.remove<Double>("poiLng")
            pois.find {
                it.name == name &&
                    abs(it.lat - lat) < 0.00001 &&
                    abs(it.lng - lng) < 0.00001
            }?.let { poi ->
                selectedPoiId = poi.id
                query = poi.name
                unsavedPoint = null
                unsavedAddress = null
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            pendingPoi = null
        }
    }


    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.declare_route),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            val bubbleState = LocalKeyboardBubbleState.current!!
            if (!isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    onMapClick = { latLng ->
                        if (selectingPoint) {
                            selectingPoint = false
                            if (heraklionBounds.contains(latLng)) {
                                val existing = pois.find {
                                    abs(it.lat - latLng.latitude) < 0.00001 &&
                                        abs(it.lng - latLng.longitude) < 0.00001
                                }
                                if (existing != null) {
                                    selectedPoiId = existing.id
                                    unsavedPoint = null
                                    unsavedAddress = null
                                    query = existing.name
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    scope.launch {
                                        val address = reverseGeocodePoint(context, latLng)
                                        unsavedPoint = latLng
                                        routeSaved = false
                                        unsavedAddress = address ?: "${latLng.latitude}, ${latLng.longitude}"
                                        query = unsavedAddress ?: ""
                                        Toast.makeText(context, context.getString(R.string.point_not_saved_toast), Toast.LENGTH_SHORT).show()
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.poi_outside_heraklion),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    routePois.forEachIndexed { index, poi ->
                        val hue = MARKER_BLUE
                        val state = rememberMarkerState(position = LatLng(poi.lat, poi.lng))
                        Marker(
                            state = state,
                            title = poi.name,
                            snippet = "Σημείο διαδρομής ${index + 1}",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                removeIndex = index
                                false
                            }
                        )
                    }
                    selectedPoi?.let { poi ->
                        if (!routePois.contains(poi)) {
                            Marker(
                                state = rememberMarkerState(position = LatLng(poi.lat, poi.lng)),
                                title = poi.name,
                                icon = BitmapDescriptorFactory.defaultMarker(MARKER_GREEN),
                                onClick = { false }
                            )
                        }
                    }
                    unsavedPoint?.let { latLng ->
                        Marker(
                            state = rememberMarkerState(position = latLng),
                            title = unsavedAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(MARKER_RED),
                            onClick = { false }
                        )
                    }
                    if (pathPoints.isNotEmpty()) {
                        Polyline(
                            points = pathPoints,
                            color = if (routeSaved) Color.Green else Color.Red
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.map_api_key_missing),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LegendTable(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = addressMenuExpanded,
                onExpandedChange = { addressMenuExpanded = !addressMenuExpanded }
            ) {
                OutlinedTextField(
                    value = addressQuery,
                    onValueChange = { queryText ->
                        addressQuery = queryText
                        if (queryText.length >= 3) {
                            scope.launch {
                                addressResults = MapsUtils.autocompleteHeraklion(queryText, apiKey)
                                addressMenuExpanded = true
                            }
                        } else {
                            addressResults = emptyList()
                        }
                    },
                    label = { Text(stringResource(R.string.search_address)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addressMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 0) { addressQuery },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = addressMenuExpanded,
                    onDismissRequest = { addressMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    addressResults.forEach { suggestion ->
                        DropdownMenuItem(text = { Text(suggestion) }, onClick = {
                            scope.launch {
                                addressQuery = suggestion
                                addressMenuExpanded = false
                                selectedPoiId = null
                                val res = geocodeHeraklion(context, suggestion).firstOrNull()
                                if (res != null) {
                                    unsavedPoint = LatLng(res.latitude, res.longitude)
                                    unsavedAddress = suggestion
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(unsavedPoint!!, 13.79f)
                                }
                                query = suggestion
                                routeSaved = false
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(query) { menuExpanded = query.isNotBlank() }
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                    menuExpanded = if (menuExpanded) false else query.isNotBlank()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    label = { Text(stringResource(R.string.add_point)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    trailingIcon = {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reset_field),
                                modifier = Modifier.clickable {
                                    query = ""
                                    selectedPoiId = null
                                    unsavedPoint = null
                                    unsavedAddress = null
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.clickable { goToCurrentLocation() }
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (unsavedPoint != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (unsavedPoint != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .observeBubble(bubbleState, 1) { query }
                )

                val filtered = if (query.isNotBlank()) {
                    val q = query.lowercase()
                    pois.filter { poi ->
                        poi.name.contains(q, true) ||
                            poi.address.country.contains(q, true) ||
                            poi.address.city.contains(q, true) ||
                            poi.address.streetName.contains(q, true) ||
                            poi.address.streetNum.toString().contains(q) ||
                            poi.address.postalCode.toString().contains(q)
                    }.sortedBy { it.name }
                } else emptyList()
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .heightIn(max = 56.dp * 3f)
                            .verticalScroll(scrollState)
                            .fillMaxWidth()
                    ) {
                        filtered.forEach { poi ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(poi.name)
                                        val a = poi.address
                                        val addressLine = buildString {
                                            if (a.streetName.isNotBlank()) append(a.streetName)
                                            if (a.streetNum != 0) append(" ${a.streetNum}")
                                            if (a.postalCode != 0 || a.city.isNotBlank()) {
                                                if (isNotEmpty()) append(", ")
                                                append("${a.postalCode} ${a.city}".trim())
                                            }
                                        }
                                        if (addressLine.isNotBlank()) {
                                            Text(addressLine, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPoiId = poi.id
                                    query = poi.name
                                    menuExpanded = false
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = {
                    selectingPoint = true
                }) { Icon(Icons.Default.Place, contentDescription = null) }
                IconButton(
                    onClick = {
                        when {
                            selectedPoi != null && unsavedPoint == null -> {
                                val added = routeViewModel.addPoiToCurrentRoute(selectedPoi!!)
                                if (!added) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.poi_already_last),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    routeSaved = false
                                }
                            }
                            unsavedPoint != null -> Toast.makeText(context, context.getString(R.string.point_not_saved_toast), Toast.LENGTH_SHORT).show()
                        }
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    enabled = selectedPoi != null || unsavedPoint != null
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
                if (removeIndex != null) {
                    IconButton(onClick = {
                        removeIndex?.let { index ->
                            val removed = routePois.getOrNull(index)
                            routeViewModel.removePoiAt(index)
                            routeSaved = false
                            if (removed?.id == selectedPoiId) {
                                selectedPoiId = null
                            }
                        }
                        removeIndex = null
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_point)
                        )
                    }
                }
                IconButton(onClick = {
                    selectedPoi?.let { navController.navigate("definePoi?lat=${it.lat}&lng=${it.lng}&source=announce&view=true") }
                        ?: unsavedPoint?.let { navController.navigate("definePoi?lat=${it.latitude}&lng=${it.longitude}&source=announce&view=true") }
                }) { Icon(Icons.Default.Search, contentDescription = null) }
                IconButton(onClick = {
                    unsavedPoint?.let { navController.navigate("definePoi?lat=${it.latitude}&lng=${it.longitude}&source=announce&view=false") }
                }, enabled = unsavedPoint != null) {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
                IconButton(onClick = {
                    val ids = routePois.map { it.id }
                    if (ids.size >= 2) {
                        calculating = true
                        scope.launch {
                            val start = LatLng(routePois.first().lat, routePois.first().lng)
                            val end = LatLng(routePois.last().lat, routePois.last().lng)
                            val waypoints = routePois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                            val data = MapsUtils.fetchDurationAndPath(
                                start,
                                end,
                                apiKey,
                                VehicleType.CAR,
                                waypoints
                            )
                            if (data.status == "OK") {
                                pathPoints.clear()
                                pathPoints.addAll(data.points)
                                routeSaved = false
                            }
                            calculating = false
                        }
                    }
                }) { Icon(Icons.Default.Directions, contentDescription = null) }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it },
                label = { Text(stringResource(R.string.route_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 2) { routeName },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Button(
                onClick = {
                    val ids = routePois.map { it.id }
                    scope.launch {
                        if (ids.size >= 2 && routeName.isNotBlank()) {
                            val newId = routeViewModel.addRoute(context, ids, routeName)
                            if (newId != null) {
                                routeSaved = true
                                selectedPoiId = null
                                unsavedPoint = null
                                unsavedAddress = null
                                Toast.makeText(context, context.getString(R.string.route_saved_success), Toast.LENGTH_SHORT).show()
                                navController.previousBackStackEntry?.savedStateHandle?.set("newRouteId", newId)
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, context.getString(R.string.route_exists), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = routePois.size >= 2 && routeName.isNotBlank()
            ) {
                Text(stringResource(R.string.save_route))
            }

            if (calculating) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.calculating_route))
                }
            }
        }
    }
}

@Composable
private fun MarkerLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LineLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 24.dp, height = 2.dp)) {
            drawRect(color = color)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegendTable(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.legend_header),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(4.dp))
        MarkerLegendItem(Color.Red, stringResource(R.string.legend_unsaved_point))
        MarkerLegendItem(Color.Green, stringResource(R.string.legend_saved_poi))
        MarkerLegendItem(Color.Blue, stringResource(R.string.legend_route_point))
        LineLegendItem(Color.Red, stringResource(R.string.legend_unsaved_route))
        LineLegendItem(Color.Green, stringResource(R.string.legend_saved_route))
    }
}

private suspend fun reverseGeocodePoint(context: Context, latLng: LatLng): String? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()?.getAddressLine(0)
    } catch (e: Exception) {
        null
    }
}

private suspend fun geocodeHeraklion(
    context: Context,
    query: String
): List<Address> = withContext(Dispatchers.IO) {
    try {
        Geocoder(context).getFromLocationName(
            query,
            5,
            35.28,
            25.05,
            35.40,
            25.20
        ) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
