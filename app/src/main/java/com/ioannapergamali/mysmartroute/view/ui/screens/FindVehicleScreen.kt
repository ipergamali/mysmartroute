package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()

    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routePois by remember { mutableStateOf<List<PoIEntity>>(emptyList()) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromIndex by remember { mutableStateOf<Int?>(null) }
    var selectedToIndex by remember { mutableStateOf<Int?>(null) }
    var maxCostText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }
    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { id ->
            routePois = routeViewModel.getRoutePois(context, id)
            selectedFromIndex = null
            selectedToIndex = null
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.find_vehicle),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = !routeExpanded }) {
                val selectedRoute = routes.find { it.id == selectedRouteId }
                OutlinedTextField(
                    value = selectedRoute?.name ?: "",
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
                ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = !fromExpanded }) {
                    val startText = selectedFromIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: ""
                    OutlinedTextField(
                        value = startText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.boarding_stop)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        routePois.forEachIndexed { index, poi ->
                            DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                                selectedFromIndex = index
                                fromExpanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = !toExpanded }) {
                    val endText = selectedToIndex?.let { "${it + 1}. ${routePois[it].name}" } ?: ""
                    OutlinedTextField(
                        value = endText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dropoff_stop)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                        routePois.forEachIndexed { index, poi ->
                            DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                                selectedToIndex = index
                                toExpanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = maxCostText,
                onValueChange = { maxCostText = it },
                label = { Text(stringResource(R.string.max_cost)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val fromIdx = selectedFromIndex ?: return@Button
                    val toIdx = selectedToIndex ?: return@Button
                    if (fromIdx >= toIdx) {
                        message = context.getString(R.string.invalid_stop_order)
                        return@Button
                    }
                    val fromId = routePois[fromIdx].id
                    val toId = routePois[toIdx].id
                    val cost = maxCostText.toDoubleOrNull() ?: Double.MAX_VALUE
                    requestViewModel.requestTransport(context, fromId, toId, cost)
                    message = context.getString(R.string.request_sent)
                },
                enabled = selectedRouteId != null && selectedFromIndex != null && selectedToIndex != null
            ) {
                Text(stringResource(R.string.find_vehicle))
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
