package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteModeScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val allPois by poiViewModel.pois.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val routePoiIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<String>() }
    val routePois = routePoiIds.mapNotNull { id -> allPois.find { it.id == id } }
    var startIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var endIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val selectedDateText = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: stringResource(R.string.select_date)

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        poiViewModel.loadPois(context)
    }
    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { id ->
            routePoiIds.clear()
            routePoiIds.addAll(routeViewModel.getRoutePois(context, id).map { it.id })
            startIndex = null
            endIndex = null
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.route_mode),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = !routeExpanded }) {
                OutlinedTextField(
                    value = routes.find { it.id == selectedRouteId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_route)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRouteId = route.id
                            routeExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (routePois.isNotEmpty()) {
                Text(stringResource(R.string.stops_header))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.poi_name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.poi_type), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    Divider()
                    routePois.forEachIndexed { index, poi ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${poi.name}", modifier = Modifier.weight(1f))
                            Text(poi.type.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (endIndex == null || index < endIndex!!) {
                                    startIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) { Text("\uD83C\uDD95") }
                            IconButton(onClick = {
                                if (startIndex == null || index > startIndex!!) {
                                    endIndex = index
                                } else {
                                    message = context.getString(R.string.invalid_stop_order)
                                }
                            }) { Text("\uD83D\uDD1A") }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = startIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: "",
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
                    value = endIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: "",
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

            Button(onClick = { showDatePicker = true }) { Text(selectedDateText) }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) { DatePicker(state = datePickerState) }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = selectedRouteId != null && startIndex != null && endIndex != null && datePickerState.selectedDateMillis != null,
                onClick = {
                    val fromIdx = startIndex ?: return@Button
                    val toIdx = endIndex ?: return@Button
                    if (fromIdx >= toIdx) {
                        message = context.getString(R.string.invalid_stop_order)
                        return@Button
                    }
                    val fromId = routePois[fromIdx].id
                    val toId = routePois[toIdx].id
                    val routeId = selectedRouteId ?: return@Button
                    val dateMillis = datePickerState.selectedDateMillis ?: 0L
                    navController.navigate(
                        "availableTransports?routeId=" +
                                routeId +
                                "&startId=" + fromId +
                                "&endId=" + toId +
                                "&maxCost=&date=" + dateMillis
                    )
                }
            ) { Text(stringResource(R.string.find_now)) }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}

