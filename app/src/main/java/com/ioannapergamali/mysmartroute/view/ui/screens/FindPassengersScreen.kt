package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.menuAnchor
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.*
import java.util.Date
import android.text.format.DateFormat
import com.ioannapergamali.mysmartroute.utils.ATHENS_TIME_ZONE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPassengersScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val transferViewModel: TransferRequestViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val movings by requestViewModel.movings.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    val selectedRequests = remember { mutableStateMapOf<String, Boolean>() }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routeExpanded by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTimeMillis by remember { mutableStateOf<Long?>(null) }
    val selectedTimeText = selectedTimeMillis?.let {
        String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
    } ?: stringResource(R.string.select_time)
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        requestViewModel.loadPassengerMovings(context, allUsers = true)
        poiViewModel.loadPois(context)
        declarationViewModel.loadDeclarations(context)
        routeViewModel.loadRoutes(context, includeAll = true)
    }

    LaunchedEffect(movings) {
        movings.forEach { req ->
            if (userNames[req.userId] == null) {
                userNames[req.userId] = userViewModel.getUserName(context, req.userId)
            }
        }
    }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDateMillis = datePickerState.selectedDateMillis
    val dateFormatter = remember(context) {
        DateFormat.getDateFormat(context).apply { timeZone = ATHENS_TIME_ZONE }
    }
    val selectedDateText = selectedDateMillis?.let { dateFormatter.format(Date(it)) }
        ?: stringResource(R.string.select_date)

    val dayMillis = 24 * 60 * 60 * 1000L
    val filteredMovings = movings.filter { req ->
        val reqDate = req.date - (req.date % dayMillis)
        val reqTime = req.date % dayMillis
        (selectedRouteId == null || req.routeId == selectedRouteId) &&
            (selectedDateMillis == null || reqDate == selectedDateMillis) &&
            (selectedTimeMillis == null || reqTime == selectedTimeMillis)
    }

    LaunchedEffect(selectedDateMillis, selectedTimeMillis, selectedRouteId) {
        showResults = false
    }

    val poiNames = pois.associate { it.id to it.name }
    val hasSelection = selectedRequests.values.any { it }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.find_passengers),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        val routeNames = routes.associate { it.id to it.name }
        val declarationRouteIds = declarations.mapNotNull { it.routeId.takeIf(String::isNotBlank) }
        val requestRouteIds = movings.mapNotNull { it.routeId.takeIf(String::isNotBlank) }
        val routeOptions = (declarationRouteIds + requestRouteIds)
            .distinct()
            .associateWith { routeId ->
                routeNames[routeId]
                    ?: movings.firstOrNull { it.routeId == routeId }?.routeName
                    ?: routeId
            }

        LaunchedEffect(routeOptions) {
            if (selectedRouteId != null && selectedRouteId !in routeOptions) {
                selectedRouteId = null
            }
        }

        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            Button(onClick = { showDatePicker = true }) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.date)
                )
                Spacer(Modifier.width(8.dp))
                Text(selectedDateText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showTimePicker = true }) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = stringResource(R.string.time)
                )
                Spacer(Modifier.width(8.dp))
                Text(selectedTimeText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = routeExpanded,
                onExpandedChange = {
                    if (routeOptions.isNotEmpty()) {
                        routeExpanded = !routeExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = routeOptions[selectedRouteId] ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    enabled = routeOptions.isNotEmpty()
                )
                if (routeOptions.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                        routeOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedRouteId = id
                                    routeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (routeOptions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_routes_available),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showResults = true },
                enabled = selectedRouteId != null && selectedDateMillis != null && selectedTimeMillis != null
            ) {
                Text(stringResource(R.string.search))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showResults) {
                if (filteredMovings.isEmpty()) {
                    Text(stringResource(R.string.no_requests))
                } else {
                    LazyColumn {
                        items(filteredMovings) { req ->
                            val passengerName = userNames[req.userId] ?: ""
                            val routeName = listOfNotNull(
                                poiNames[req.startPoiId],
                                poiNames[req.endPoiId]
                            ).joinToString(" - ")
                            val isChecked = selectedRequests[req.id] ?: false
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedRequests[req.id] = true
                                        else selectedRequests.remove(req.id)
                                    }
                                )
                                Column {
                                    Text(passengerName)
                                    Text(routeName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val ids = selectedRequests.filterValues { it }.keys
                            ids.forEach { id ->
                                val req = filteredMovings.find { it.id == id }
                                if (req != null) {
                                    requestViewModel.notifyRoute(context, id)
                                    transferViewModel.notifyDriver(context, req.requestNumber)
                                }
                            }
                            selectedRequests.clear()
                        },
                        enabled = hasSelection
                    ) {
                        Text(stringResource(R.string.notify_selected))
                    }

                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimeMillis = (timePickerState.hour * 60 + timePickerState.minute) * 60 * 1000L
                    showTimePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

