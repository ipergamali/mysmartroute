package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import android.location.Geocoder
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.utils.PlacesHelper
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import androidx.compose.material3.menuAnchor

private const val TAG = "DefinePoiScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinePoiScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    initialLat: Double? = null,
    initialLng: Double? = null,
    source: String? = null,
    viewOnly: Boolean = false
) {
    val viewModel: PoIViewModel = viewModel()
    val addState by viewModel.addState.collectAsState()
    val context = LocalContext.current
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
        LatLngBounds(LatLng(34.9, 24.8), LatLng(35.5, 25.9))
    }
    val mapProperties = remember { MapProperties(latLngBoundsForCameraTarget = heraklionBounds) }

    LaunchedEffect(Unit) { viewModel.loadPois(context) }


    LaunchedEffect(addState) {
        when (addState) {
            PoIViewModel.AddPoiState.Success -> {
                Toast.makeText(context, context.getString(R.string.poi_saved), Toast.LENGTH_SHORT).show()
                selectedLatLng?.let { latLng ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
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
            if (MapsUtils.getApiKey(context).isNotBlank()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    onMapClick = { latLng ->
                        Log.d(TAG, "Map clicked at ${latLng.latitude}, ${latLng.longitude}")
                        selectedLatLng = latLng
                        markerState.position = latLng
                    }
                ) {
                    selectedLatLng?.let { Marker(state = markerState) }
                }
                LaunchedEffect(selectedLatLng) {
                    selectedLatLng?.let { latLng ->
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13.79f)
                        val place = MapsUtils.fetchNearbyPlaceName(
                            latLng,
                            MapsUtils.getApiKey(context)
                        )
                        Log.d(TAG, "Nearby place name: $place")
                        if (!place.isNullOrBlank()) {
                            name = place
                        }
                        val type = MapsUtils.fetchNearbyPlaceType(
                            latLng,
                            MapsUtils.getApiKey(context)
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



            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.poi_name)) },
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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

