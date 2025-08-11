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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.*
import java.util.Date
import android.text.format.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPassengersScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val transferViewModel: TransferRequestViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()

    val requests by requestViewModel.requests.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    val selectedRequests = remember { mutableStateMapOf<String, Boolean>() }
    val driverId = FirebaseAuth.getInstance().currentUser?.uid
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routeExpanded by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTimeMillis by remember { mutableStateOf<Long?>(null) }
    val selectedTimeText = selectedTimeMillis?.let {
        String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
    } ?: stringResource(R.string.select_time)

    LaunchedEffect(Unit) {
        requestViewModel.loadRequests(context, allUsers = true)
        declarationViewModel.loadDeclarations(context, driverId)
        poiViewModel.loadPois(context)
        routeViewModel.loadRoutes(context)
    }

    LaunchedEffect(requests) {
        requests.forEach { req ->
            if (userNames[req.userId] == null) {
                userNames[req.userId] = userViewModel.getUserName(context, req.userId)
            }
        }
    }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDateMillis = datePickerState.selectedDateMillis
    val dateFormatter = remember { DateFormat.getDateFormat(context) }
    val selectedDateText = selectedDateMillis?.let { dateFormatter.format(Date(it)) }
        ?: stringResource(R.string.select_date)

    val filteredDeclarations = remember(declarations, selectedDateMillis, selectedTimeMillis, selectedRouteId) {
        declarations.filter { decl ->
            (selectedDateMillis == null || decl.date == selectedDateMillis) &&
            (selectedTimeMillis == null || decl.startTime == selectedTimeMillis) &&
            (selectedRouteId == null || decl.routeId == selectedRouteId)
        }
    }
    val routeIds = filteredDeclarations.map { it.routeId }.toSet()
    val filteredRequests = requests.filter { req ->
        routeIds.contains(req.routeId) &&
            (selectedDateMillis == null || req.date == selectedDateMillis) &&
            req.status != "completed"
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
        val routeMap = routes.associate { it.id to it.name }
        val routeOptions = routeMap.filterKeys { id -> declarations.any { it.routeId == id } }

        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            Button(onClick = { showDatePicker = true }) {
                Text(selectedDateText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showTimePicker = true }) {
                Text(selectedTimeText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (routeOptions.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = !routeExpanded }) {
                    OutlinedTextField(
                        value = routeOptions[selectedRouteId] ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.route)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true
                    )
                    ExposedDropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                        routeOptions.forEach { (id, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                selectedRouteId = id
                                routeExpanded = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (filteredRequests.isEmpty()) {
                Text(stringResource(R.string.no_requests))
            } else {
                LazyColumn {
                    items(filteredRequests) { req ->
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
                            val req = filteredRequests.find { it.id == id }
                            if (req != null) {
                                requestViewModel.notifyPassenger(context, id)
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

