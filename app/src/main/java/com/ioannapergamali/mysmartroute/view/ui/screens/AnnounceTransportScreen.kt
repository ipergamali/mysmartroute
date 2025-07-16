package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeViewModel: RouteViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context) }

    var expandedRoute by remember { mutableStateOf(false) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var costText by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(0) }

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.announce_availability),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Box {
                OutlinedTextField(
                    value = routes.firstOrNull { it.id == selectedRouteId }?.name ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.route)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable { expandedRoute = true },
                    readOnly = true
                )
                DropdownMenu(expanded = expandedRoute, onDismissRequest = { expandedRoute = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(text = { Text(route.name) }, onClick = {
                            selectedRouteId = route.id
                            expandedRoute = false
                            scope.launch {
                                val points = routeViewModel.getPointsCount(context, route.id)
                                duration = points * 5
                            }
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box {
                OutlinedTextField(
                    value = selectedVehicle?.name ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.vehicle)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable { expandedVehicle = true },
                    readOnly = true
                )
                DropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                    VehicleType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.name) }, onClick = {
                            selectedVehicle = type
                            expandedVehicle = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text(stringResource(R.string.cost)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.duration) + ": $duration")

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val routeId = selectedRouteId
                    val vehicle = selectedVehicle
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    if (routeId != null && vehicle != null) {
                        declarationViewModel.declareTransport(context, routeId, vehicle, cost, duration)
                        navController.popBackStack()
                    }
                },
                enabled = selectedRouteId != null && selectedVehicle != null
            ) {
                Text(stringResource(R.string.announce))
            }
        }
    }
}
