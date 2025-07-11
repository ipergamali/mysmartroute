package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import android.widget.Toast
import android.location.Geocoder
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Polyline
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
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AnnounceTransport"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
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

    val heraklionBounds = LatLngBounds(LatLng(34.9, 24.8), LatLng(35.5, 25.9))
    val mapProperties = MapProperties(latLngBoundsForCameraTarget = heraklionBounds)

    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()
    Log.d(TAG, "API key loaded? ${!isKeyMissing}")

    val routePois by routeViewModel.currentRoute.collectAsState()
    val pathPoints = remember { mutableStateListOf<LatLng>() }
    var menuExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedPoi by remember { mutableStateOf<PoIEntity?>(null) }
    var selectingPoint by remember { mutableStateOf(false) }
    var unsavedPoint by remember { mutableStateOf<LatLng?>(null) }
    var unsavedAddress by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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
            pois.find { it.name == name && it.lat == lat && it.lng == lng }?.let { poi ->
                selectedPoi = poi
                query = poi.name
                routeViewModel.addPoiToCurrentRoute(poi)
            }
            pendingPoi = null
        }
    }


    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.announce_transport),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (!isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    onMapClick = { latLng ->
                        if (selectingPoint) {
                            selectingPoint = false
                            val existing = pois.find { it.lat == latLng.latitude && it.lng == latLng.longitude }
                            if (existing != null) {
                                selectedPoi = existing
                                unsavedPoint = null
                                unsavedAddress = null
                                query = existing.name
                            } else {
                                scope.launch {
                                    val address = reverseGeocodePoint(context, latLng)
                                    unsavedPoint = latLng
                                    unsavedAddress = address ?: "${latLng.latitude}, ${latLng.longitude}"
                                    query = ""
                                    Toast.makeText(context, context.getString(R.string.point_not_saved_toast), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    routePois.forEachIndexed { index, poi ->
                        val hue = when (index) {
                            0 -> BitmapDescriptorFactory.HUE_GREEN
                            routePois.lastIndex -> BitmapDescriptorFactory.HUE_RED
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }
                        Marker(
                            state = MarkerState(LatLng(poi.lat, poi.lng)),
                            title = poi.name,
                            icon = BitmapDescriptorFactory.defaultMarker(hue)
                        )
                    }
                    if (pathPoints.isNotEmpty()) {
                        Polyline(points = pathPoints)
                    }
                }
            } else {
                Text(
                    stringResource(R.string.map_api_key_missing),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(query) { menuExpanded = query.isNotBlank() }
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = {
                    focusRequester.requestFocus()
                    menuExpanded = if (menuExpanded) false else query.isNotBlank()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedPoi = selectedPoi?.copy(name = it)
                    },
                    label = { Text(stringResource(R.string.add_point)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.clickable { goToCurrentLocation() }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (unsavedPoint != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (unsavedPoint != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
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
                                    selectedPoi = poi
                                    query = poi.name
                                    menuExpanded = false
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
                IconButton(onClick = { selectedPoi?.let { routeViewModel.addPoiToCurrentRoute(it) } }, enabled = selectedPoi != null && unsavedPoint == null) {
                    Icon(Icons.Default.Check, contentDescription = null)
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
                        routeViewModel.addRoute(context, ids)
                        scope.launch {
                            val start = LatLng(routePois.first().lat, routePois.first().lng)
                            val end = LatLng(routePois.last().lat, routePois.last().lng)
                            val waypoints = routePois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                            val data = MapsUtils.fetchDurationAndPath(
                                start,
                                end,
                                apiKey,
                                com.ioannapergamali.mysmartroute.model.enumerations.VehicleType.CAR,
                                waypoints
                            )
                            if (data.status == "OK") {
                                pathPoints.clear()
                                pathPoints.addAll(data.points)
                            }
                        }
                    }
                }) { Icon(Icons.Default.Directions, contentDescription = null) }
            }
        }
    }
}

private suspend fun reverseGeocodePoint(context: Context, latLng: LatLng): String? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()?.getAddressLine(0)
    } catch (e: Exception) {
        null
    }
}
