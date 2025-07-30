package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val poiViewModel: PoIViewModel = viewModel()
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val pois by poiViewModel.pois.collectAsState()

    var fromQuery by remember { mutableStateOf("") }
    var toQuery by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFrom by remember { mutableStateOf<PoIEntity?>(null) }
    var selectedTo by remember { mutableStateOf<PoIEntity?>(null) }
    var maxCostText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { poiViewModel.loadPois(context) }

    val filteredFrom = if (fromQuery.isBlank()) pois else pois.filter {
        it.name.contains(fromQuery, true) ||
            it.address.city.contains(fromQuery, true) ||
            it.address.streetName.contains(fromQuery, true)
    }
    val filteredTo = if (toQuery.isBlank()) pois else pois.filter {
        it.name.contains(toQuery, true) ||
            it.address.city.contains(toQuery, true) ||
            it.address.streetName.contains(toQuery, true)
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
            ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = !fromExpanded }) {
                OutlinedTextField(
                    value = selectedFrom?.name ?: fromQuery,
                    onValueChange = { fromQuery = it; selectedFrom = null },
                    label = { Text(stringResource(R.string.start_point)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                    filteredFrom.forEach { poi ->
                        DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                            selectedFrom = poi
                            fromQuery = poi.name
                            fromExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { navController.navigate("definePoi?lat=&lng=&source=from&view=false") }) {
                Text(stringResource(R.string.add_point))
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = !toExpanded }) {
                OutlinedTextField(
                    value = selectedTo?.name ?: toQuery,
                    onValueChange = { toQuery = it; selectedTo = null },
                    label = { Text(stringResource(R.string.destination)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                    filteredTo.forEach { poi ->
                        DropdownMenuItem(text = { Text(poi.name) }, onClick = {
                            selectedTo = poi
                            toQuery = poi.name
                            toExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { navController.navigate("definePoi?lat=&lng=&source=to&view=false") }) {
                Text(stringResource(R.string.add_point))
            }

            Spacer(Modifier.height(16.dp))

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
                    val fromId = selectedFrom?.id ?: return@Button
                    val toId = selectedTo?.id ?: return@Button
                    val cost = maxCostText.toDoubleOrNull() ?: Double.MAX_VALUE
                    requestViewModel.requestTransport(context, fromId, toId, cost)
                    message = context.getString(R.string.request_sent)
                },
                enabled = selectedFrom != null && selectedTo != null
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
