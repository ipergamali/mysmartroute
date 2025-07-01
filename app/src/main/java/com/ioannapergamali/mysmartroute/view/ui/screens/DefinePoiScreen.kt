package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import android.location.Geocoder
import android.content.Context
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
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.model.enumerations.PoIType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinePoiScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: PoIViewModel = viewModel()
    val pois by viewModel.pois.collectAsState()
    val addState by viewModel.addState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<PoIEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(PoIType.HISTORICAL) }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var streetName by remember { mutableStateOf("") }
    var streetNumInput by remember { mutableStateOf("") }
    var postalCodeInput by remember { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val markerState = rememberMarkerState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.3325932, 25.073835), 13.79f)
    }
    val heraklionBounds = remember {
        LatLngBounds(LatLng(34.9, 24.8), LatLng(35.5, 25.9))
    }
    val mapProperties = remember { MapProperties(latLngBoundsForCameraTarget = heraklionBounds) }

    LaunchedEffect(Unit) { viewModel.loadPois(context) }

    LaunchedEffect(selectedPoi) {
        selectedPoi?.let { poi ->
            val latLng = LatLng(poi.lat, poi.lng)
            selectedLatLng = latLng
            markerState.position = latLng
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)
            name = poi.name
            country = poi.country
            city = poi.city
            streetName = poi.streetName
            streetNumInput = poi.streetNum.toString()
            postalCodeInput = poi.postalCode.toString()
        }
    }

    LaunchedEffect(addState) {
        when (addState) {
            PoIViewModel.AddPoiState.Success -> {
                Toast.makeText(context, context.getString(R.string.poi_saved), Toast.LENGTH_SHORT).show()
                viewModel.resetAddState()
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
                        selectedLatLng = latLng
                        markerState.position = latLng
                        coroutineScope.launch {
                            reverseGeocodePoi(context, latLng)?.let { address ->
                                streetName = address.thoroughfare ?: ""
                                streetNumInput = address.subThoroughfare ?: ""
                                city = address.locality ?: ""
                                postalCodeInput = address.postalCode ?: ""
                                country = address.countryName ?: ""
                            }
                        }
                    }
                ) {
                    selectedLatLng?.let { Marker(state = markerState) }
                }
            } else {
                Text(stringResource(R.string.map_api_key_missing))
            }

            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedPoi?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_poi)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    pois.forEach { poi ->
                        DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                            selectedPoi = poi
                            expanded = false
                        })
                    }
                }
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
                    value = selectedType.name,
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
                DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    PoIType.values().forEach { t ->
                        DropdownMenuItem(text = { Text(t.name) }, onClick = {
                            selectedType = t
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
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val latLng = selectedLatLng
                val streetNum = streetNumInput.toIntOrNull() ?: 0
                val postalCode = postalCodeInput.toIntOrNull() ?: 0
                if (name.isNotBlank() && latLng != null) {
                    viewModel.addPoi(
                        context,
                        name,
                        PoiAddress(country, city, streetName, streetNum, postalCode),
                        selectedType,
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

