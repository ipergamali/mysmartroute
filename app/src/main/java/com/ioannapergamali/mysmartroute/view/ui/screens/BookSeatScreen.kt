package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ioannapergamali.mysmartroute.model.enumerations.BookingStep
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.BookingStepsIndicator
import com.ioannapergamali.mysmartroute.viewmodel.BookingViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransferRequestViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val MARKER_ORANGE = BitmapDescriptorFactory.HUE_ORANGE
private const val MARKER_BLUE = BitmapDescriptorFactory.HUE_BLUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSeatScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    @StringRes titleRes: Int = R.string.book_seat,
    restrictToAvailableDates: Boolean = true
) {
    val viewModel: BookingViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val transferRequestViewModel: TransferRequestViewModel = viewModel()
    val routes by viewModel.availableRoutes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRoute = routes.find { it.id == selectedRouteId }
    var message by remember { mutableStateOf("") }
    val poiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val userPoiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val originalPoiIds = remember { mutableStateListOf<String>() }
    var startIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var endIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val pois = poiIds.mapNotNull { id -> allPois.find { it.id == id } }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculating by remember { mutableStateOf(false) }
    var pendingPoi by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }

    val availableDates = if (restrictToAvailableDates) {
        remember(declarations, selectedRouteId) {
            declarations.filter { it.routeId == selectedRouteId }
                .map {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                .toSet()
        }
    } else emptySet()

    val datePickerState = if (restrictToAvailableDates) {
        rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(dateMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    return availableDates.contains(date)
                }
            }
        )
    } else {
        rememberDatePickerState()
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)
    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current
    val apiKey = MapsUtils.getApiKey(context)
    val isKeyMissing = apiKey.isBlank()

    fun refreshRoute() {
        val currentPois = poiIds.mapNotNull { id -> allPois.find { it.id == id } }
        if (currentPois.size >= 2) {
            scope.launch {
                calculating = true
                val origin = LatLng(currentPois.first().lat, currentPois.first().lng)
                val destination = LatLng(currentPois.last().lat, currentPois.last().lng)
                val waypoints = currentPois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                val data = MapsUtils.fetchDurationAndPath(
                    origin,
                    destination,
                    apiKey,
                    VehicleType.CAR,
                    waypoints
                )
                pathPoints = data.points
                data.points.firstOrNull()?.let {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(it, 13f)
                    )
                }
                calculating = false
            }
        } else {
            pathPoints = emptyList()
        }
    }

    suspend fun saveEditedRouteIfChanged(): String {
        if (poiIds == originalPoiIds) return selectedRouteId ?: ""
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return selectedRouteId ?: ""
        val username = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
            .getString("username") ?: uid
        val baseName = routes.find { it.id == selectedRouteId }?.name ?: "route"
        return routeViewModel.addRoute(
            context,
            poiIds.toList(),
            "${baseName}_edited_by_$username"
        ) ?: selectedRouteId ?: ""
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

                val newRoute = savedStateHandle?.get<String>("newRouteId")
                if (newRoute != null) {
                    savedStateHandle.remove<String>("newRouteId")
                    selectedRouteId = newRoute
                    viewModel.refreshRoutes()
                    routeViewModel.loadRoutes(context, includeAll = true)
                    scope.launch {
                        val (_, path) = routeViewModel.getRouteDirections(
                            context,
                            newRoute,
                            VehicleType.CAR
                        )
                        pathPoints = path
                        poiIds.clear()
                        userPoiIds.clear()
                        originalPoiIds.clear()
                        startIndex = null
                        endIndex = null
                        poiIds.addAll(routeViewModel.getRoutePois(context, newRoute).map { it.id })
                        originalPoiIds.addAll(poiIds)
                        path.firstOrNull()?.let {
                            MapsInitializer.initialize(context)
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngZoom(it, 13f)
                            )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allPois, pendingPoi) {
        pendingPoi?.let { (name, lat, lng) ->
            allPois.find { poi ->
                poi.name == name &&
                    abs(poi.lat - lat) < 0.00001 &&
                    abs(poi.lng - lng) < 0.00001
            }?.let { poi ->
                if (poiIds.none { it == poi.id }) {
                    savedStateHandle?.remove<String>("poiName")
                    savedStateHandle?.remove<Double>("poiLat")
                    savedStateHandle?.remove<Double>("poiLng")
                    poiIds.add(poi.id)
                    userPoiIds.add(poi.id)
                    refreshRoute()
                    pendingPoi = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
        declarationViewModel.loadDeclarations(context)
    }

    LaunchedEffect(selectedRouteId, poiIds, allPois) {
        if (selectedRouteId != null && poiIds.size >= 2 && pathPoints.isEmpty()) {
            refreshRoute()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(titleRes),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            var routeMenuExpanded by remember { mutableStateOf(false) }

            val currentStep = when {
                routes.isEmpty() -> BookingStep.DECLARE_ROUTE
                selectedRoute == null -> BookingStep.SELECT_ROUTE
                poiIds.size < 2 -> BookingStep.ADD_POI
                pathPoints.isEmpty() -> BookingStep.RECALCULATE_ROUTE
                datePickerState.selectedDateMillis == null -> BookingStep.SELECT_DATE
                else -> BookingStep.RESERVE_SEAT
            }

            BookingStepsIndicator(currentStep = currentStep)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("declareRoute") }) {
                Text(stringResource(R.string.declare_route))
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = routeMenuExpanded,
                onExpandedChange = { routeMenuExpanded = !routeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                selectedRouteId = route.id
                                routeMenuExpanded = false
                                scope.launch {
                                    val (_, path) = routeViewModel.getRouteDirections(
                                        context,
                                        route.id,
                                        VehicleType.CAR
                                    )
                                    pathPoints = path
                                    poiIds.clear()
                                    userPoiIds.clear()
                                    originalPoiIds.clear()
                                    startIndex = null
                                    endIndex = null
                                    poiIds.addAll(routeViewModel.getRoutePois(context, route.id).map { it.id })
                                    originalPoiIds.addAll(poiIds)
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

            if (selectedRoute != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val rId = selectedRouteId ?: ""
                        navController.navigate("definePoi?lat=&lng=&source=&view=false&routeId=$rId")
                    }) {
                        Text(stringResource(R.string.add_poi_option))
                    }

                }

                Spacer(Modifier.height(16.dp))
            }

            if (pois.size >= 2) {
                Button(onClick = { refreshRoute() }, enabled = !calculating) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.recalculate_route))
                }

                Spacer(Modifier.height(16.dp))
            }

            if (selectedRoute != null && pathPoints.isNotEmpty() && !isKeyMissing) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.map_height)),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(points = pathPoints, color = Color.Green)
                    pois.forEach { poi ->
                        val hue = if (poi.id in userPoiIds) MARKER_ORANGE else MARKER_BLUE
                        Marker(
                            state = MarkerState(position = LatLng(poi.lat, poi.lng)),
                            title = poi.name,
                            icon = BitmapDescriptorFactory.defaultMarker(hue)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            } else if (isKeyMissing) {
                Text(stringResource(R.string.map_api_key_missing))
                Spacer(Modifier.height(16.dp))
            }

            if (pois.isNotEmpty()) {
                Text(stringResource(R.string.stops_header))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.poi_name),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            stringResource(R.string.poi_type),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Divider()
                    pois.forEachIndexed { index, poi ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${poi.name}", modifier = Modifier.weight(1f))
                            Text(poi.type.name, modifier = Modifier.weight(1f))

                            IconButton(onClick = {
                                if (endIndex == null || index < endIndex!!) {
                                    startIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83C\uDD95")
                            }
                            IconButton(onClick = {
                                if (startIndex == null || index > startIndex!!) {
                                    endIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) {
                                Text("\uD83D\uDD1A")
                            }

                            IconButton(onClick = {
                                val removedId = poiIds.removeAt(index)
                                userPoiIds.remove(removedId)
                                if (startIndex != null) {
                                    if (index == startIndex) startIndex = null else if (index < startIndex!!) startIndex = startIndex!! - 1
                                }
                                if (endIndex != null) {
                                    if (index == endIndex) endIndex = null else if (index < endIndex!!) endIndex = endIndex!! - 1
                                }
                                refreshRoute()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_point))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = startIndex?.let { "${it + 1}. ${pois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.boarding_stop)) },
                    leadingIcon = { Text("\uD83C\uDD95") },
                    trailingIcon = {
                        IconButton(onClick = { startIndex = null }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.clear_selection))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = endIndex?.let { "${it + 1}. ${pois[it].name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dropoff_stop)) },
                    leadingIcon = { Text("\uD83D\uDD1A") },
                    trailingIcon = {
                        IconButton(onClick = { endIndex = null }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.clear_selection))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))
            }

            Text(stringResource(R.string.departure_date))
            Button(onClick = { showDatePicker = true }) { Text(selectedDateText) }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = selectedRoute != null && startIndex != null && endIndex != null &&
                            datePickerState.selectedDateMillis != null,
                    onClick = {
                        val r = selectedRoute ?: return@Button
                        val dateMillis = datePickerState.selectedDateMillis ?: return@Button
                        val startId = startIndex?.let { pois[it].id } ?: return@Button
                        val endId = endIndex?.let { pois[it].id } ?: return@Button
                        requestViewModel.requestTransport(
                            context,
                            r.id,
                            startId,
                            endId,
                            Double.MAX_VALUE,
                            dateMillis
                        )
                        navController.navigate(
                            "availableTransports?routeId=" +
                                    r.id +
                                    "&startId=" + startId +
                                    "&endId=" + endId +
                                    "&maxCost=&date=" + dateMillis
                        )
                    }
                ) {
                    Text(stringResource(R.string.find_now))
                }
                Button(
                    enabled = selectedRoute != null && startIndex != null && endIndex != null &&
                            datePickerState.selectedDateMillis != null,
                    onClick = {
                        scope.launch {
                            val r = selectedRoute ?: return@launch
                            val dateMillis = datePickerState.selectedDateMillis ?: return@launch
                            val startId = startIndex?.let { pois[it].id } ?: return@launch
                            val endId = endIndex?.let { pois[it].id } ?: return@launch
                            val routeId = saveEditedRouteIfChanged()
                            requestViewModel.requestTransport(
                                context,
                                routeId,
                                startId,
                                endId,
                                Double.MAX_VALUE,
                                dateMillis
                            )
                            transferRequestViewModel.submitRequest(
                                context,
                                routeId,
                                dateMillis,
                                Double.MAX_VALUE
                            )
                            message = context.getString(R.string.request_sent)
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_request))
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
