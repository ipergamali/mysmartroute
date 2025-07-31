package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import android.location.Geocoder
import android.location.Address
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.utils.PlacesHelper
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import androidx.compose.material3.menuAnchor
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import androidx.compose.ui.graphics.Color

private const val TAG = "DefinePoiScreen"
private const val MARKER_ORANGE = BitmapDescriptorFactory.HUE_ORANGE
private const val MARKER_BLUE = BitmapDescriptorFactory.HUE_BLUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinePoiScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    initialLat: Double? = null,
    initialLng: Double? = null,
    source: String? = null,
    viewOnly: Boolean = false,
    routeId: String? = null
) {
    val viewModel: PoIViewModel = viewModel()
    val addState by viewModel.addState.collectAsState()
    val context = LocalContext.current
    val apiKey = MapsUtils.getApiKey(context)
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var selectedPlaceType by remember { mutableStateOf(Place.Type.RESTAURANT) }
    val placeTypes = remember { PlacesHelper.allPlaceTypes().sortedBy { it.name } }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var streetName by remember { mutableStateOf("") }
    var streetNumInput by remember { mutableStateOf("") }
    var postalCodeInput by remember { mutableStateOf("") }
    var addressQuery by remember { mutableStateOf("") }
    var addressResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var addressMenuExpanded by remember { mutableStateOf(false) }
    val routeViewModel: RouteViewModel = viewModel()
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routePois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    val initialLatLng = remember(initialLat, initialLng) {
        if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else null
    }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(initialLatLng) }
    val markerState = rememberMarkerState(position = selectedLatLng ?: LatLng(0.0, 0.0))

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            selectedLatLng ?: LatLng(35.3325932, 25.073835),
            13.79f
        )
    }
    val heraklionBounds = remember {
        // Περιορίζουμε την κάμερα στα όρια της πόλης του Ηρακλείου
        LatLngBounds(
            LatLng(35.28, 25.05), // νοτιοδυτικό όριο
            LatLng(35.40, 25.20)  // βορειοανατολικό όριο
        )
    }
    val mapProperties = remember { MapProperties(latLngBoundsForCameraTarget = heraklionBounds) }

    LaunchedEffect(Unit) { viewModel.loadPois(context) }

    LaunchedEffect(routeId) {
        if (!routeId.isNullOrBlank()) {
            routeViewModel.loadRoutes(context, includeAll = true)
            val pois = routeViewModel.getRoutePois(context, routeId)
            routePois = pois
            val (_, points) = routeViewModel.getRouteDirections(
                context,
                routeId,
                VehicleType.CAR
            )
            pathPoints = points
            points.firstOrNull()?.let {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }


    LaunchedEffect(addState) {
        when (val state = addState) {
            is PoIViewModel.AddPoiState.Success -> {
                Toast.makeText(context, context.getString(R.string.poi_saved), Toast.LENGTH_SHORT).show()
                selectedLatLng?.let { latLng ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        handle["poiId"] = state.id
                        handle["poiName"] = name
                        handle["poiLat"] = latLng.latitude
                        handle["poiLng"] = latLng.longitude
                        handle["poiFrom"] = source == "from"
                    }
                }
                viewModel.resetAddState()
                navController.popBackStack()
            }
            PoIViewModel.AddPoiState.Exists -> {
                Toast.makeText(context, context.getString(R.string.poi_exists), Toast.LENGTH_SHORT).show()
                viewModel.resetAddState()
            }
            else -> {}
        }
    }

    Scaffold(topBar = {
        TopBar(title = stringResource(R.string.define_poi), navController = navController, showMenu = true, onMenuClick = openDrawer)
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            val bubbleState = LocalKeyboardBubbleState.current!!
            if (apiKey.isNotBlank()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    onMapClick = { latLng ->
                        Log.d(TAG, "Map clicked at ${latLng.latitude}, ${latLng.longitude}")
                        if (heraklionBounds.contains(latLng)) {
                            selectedLatLng = latLng
                            markerState.position = latLng
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.poi_outside_heraklion),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    if (pathPoints.isNotEmpty()) {
                        Polyline(points = pathPoints, color = Color.Green)
                    }
                    routePois.forEach { poi ->
                        Marker(
                            state = MarkerState(position = LatLng(poi.lat, poi.lng)),
                            title = poi.name,
                            icon = BitmapDescriptorFactory.defaultMarker(MARKER_BLUE)
                        )
                    }
                    selectedLatLng?.let {
                        Marker(
                            state = markerState,
                            icon = BitmapDescriptorFactory.defaultMarker(MARKER_ORANGE)
                        )
                    }
                }
                LaunchedEffect(selectedLatLng) {
                    selectedLatLng?.let { latLng ->
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13.79f)
                        val place = MapsUtils.fetchNearbyPlaceName(
                            latLng,
                            apiKey
                        )
                        Log.d(TAG, "Nearby place name: $place")
                        if (!place.isNullOrBlank()) {
                            name = place
                        }
                        val type = MapsUtils.fetchNearbyPlaceType(
                            latLng,
                            apiKey
                        )
                        Log.d(TAG, "Nearby place type: ${type?.name}")
                        type?.let { selectedPlaceType = it }
                        val address = reverseGeocodePoi(context, latLng)
                        Log.d(TAG, "Reverse geocoded address: $address")
                        address?.let {
                            streetName = it.thoroughfare ?: ""
                            streetNumInput = it.subThoroughfare ?: ""
                            city = it.locality ?: ""
                            postalCodeInput = it.postalCode ?: ""
                            country = it.countryName ?: ""
                        }
                        Log.d(TAG, "Selected type after lookup: ${selectedPlaceType.name}")
                    }
                }
            } else {
                Text(stringResource(R.string.map_api_key_missing))
            }



            if (source != "announce") {
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = addressMenuExpanded,
                    onExpandedChange = { addressMenuExpanded = !addressMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = addressQuery,
                        onValueChange = { query ->
                            addressQuery = query
                            if (query.length >= 3) {
                                coroutineScope.launch {
                                    addressResults = MapsUtils.autocompleteHeraklion(query, apiKey)
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
                                coroutineScope.launch {
                                    addressQuery = suggestion
                                    addressMenuExpanded = false
                                    val addr = geocodeHeraklion(context, suggestion).firstOrNull()
                                    if (addr != null) {
                                        streetName = addr.thoroughfare ?: ""
                                        streetNumInput = addr.subThoroughfare ?: ""
                                        city = addr.locality ?: ""
                                        postalCodeInput = addr.postalCode ?: ""
                                        country = addr.countryName ?: ""
                                        selectedLatLng = LatLng(addr.latitude, addr.longitude)
                                        selectedLatLng?.let {
                                            markerState.position = it
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 13.79f)
                                        }
                                    }
                                }
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.poi_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 1) { name },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }) {
                OutlinedTextField(
                    value = selectedPlaceType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.poi_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    placeTypes.forEach { t ->
                        DropdownMenuItem(text = { Text(t.name) }, onClick = {
                            Log.d(TAG, "User selected type from menu: ${t.name}")
                            selectedPlaceType = t
                            typeMenuExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country") },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 2) { country },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 3) { city },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = streetName,
                onValueChange = { streetName = it },
                label = { Text("Street Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 4) { streetName },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = streetNumInput,
                onValueChange = { streetNumInput = it },
                label = { Text("Street Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 5) { streetNumInput },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = postalCodeInput,
                onValueChange = { postalCodeInput = it },
                label = { Text("Postal Code") },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 6) { postalCodeInput },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            if (!viewOnly) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val latLng = selectedLatLng
                    val streetNum = streetNumInput.toIntOrNull() ?: 0
                    val postalCode = postalCodeInput.filter { it.isDigit() }.toIntOrNull() ?: 0
                    if (name.isNotBlank() && latLng != null) {
                        Log.d(TAG, "Saving PoI with type ${selectedPlaceType.name}")
                        viewModel.addPoi(
                            context,
                            name,
                            PoiAddress(country, city, streetName, streetNum, postalCode),
                            Place.Type.values().firstOrNull { it.name == selectedPlaceType.name } ?: Place.Type.ESTABLISHMENT,
                            latLng.latitude,
                            latLng.longitude
                        )
                    } else {
                        Toast.makeText(context, context.getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.save_poi))
                }
            }
        }
    }
}

private suspend fun reverseGeocodePoi(
    context: Context,
    latLng: LatLng
): android.location.Address? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)
            ?.firstOrNull()
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

