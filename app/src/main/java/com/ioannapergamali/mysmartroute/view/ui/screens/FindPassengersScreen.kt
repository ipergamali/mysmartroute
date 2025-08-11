package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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

    val requests by requestViewModel.requests.collectAsState()
    val declarations by declarationViewModel.declarations.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    val selectedRequests = remember { mutableStateMapOf<String, Boolean>() }
    val driverId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        requestViewModel.loadRequests(context, allUsers = true)
        declarationViewModel.loadDeclarations(context, driverId)
        poiViewModel.loadPois(context)
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

    val filteredDeclarations = remember(declarations, selectedDateMillis) {
        declarations.filter { decl ->
            selectedDateMillis == null || decl.date == selectedDateMillis
        }
    }
    val routeIds = filteredDeclarations.map { it.routeId }.toSet()
    val filteredRequests = requests.filter { req ->
        routeIds.contains(req.routeId) &&
            (selectedDateMillis == null || req.date == selectedDateMillis)
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
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            Button(onClick = { showDatePicker = true }) {
                Text(selectedDateText)
            }
            Spacer(modifier = Modifier.height(8.dp))
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
}

