@file:OptIn(ExperimentalMaterial3Api::class)
package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.WalkingUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlinx.coroutines.launch

@Composable
fun DefineDurationScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val pendingRoutes = routes
    var routeExpanded by remember { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var durationMinutes by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { routeViewModel.loadRoutesWithoutDuration(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.define_duration),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (pendingRoutes.isEmpty()) {
                Text(stringResource(R.string.no_walking_routes))
            } else {
                ExposedDropdownMenuBox(
                    expanded = routeExpanded,
                    onExpandedChange = { routeExpanded = !routeExpanded }
                ) {
                    val selectedRoute = pendingRoutes.find { it.id == selectedRouteId }
                    OutlinedTextField(
                        value = selectedRoute?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_route)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = routeExpanded,
                        onDismissRequest = { routeExpanded = false }
                    ) {
                        pendingRoutes.forEach { route ->
                            DropdownMenuItem(
                                text = { Text(route.name) },
                                onClick = {
                                    selectedRouteId = route.id
                                    routeExpanded = false
                                    durationMinutes = null
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val rId = selectedRouteId ?: return@Button
                        coroutineScope.launch {
                            val distance = routeViewModel.getRouteDistance(context, rId)
                            durationMinutes =
                                WalkingUtils.walkingDuration(distance.toDouble()).inWholeMinutes.toInt()
                        }
                    },
                    enabled = selectedRouteId != null
                ) {
                    Text(stringResource(R.string.calculate))
                }

                durationMinutes?.let { mins ->
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.duration_format, mins))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val rId = selectedRouteId ?: return@Button
                        routeViewModel.updateWalkDuration(context, rId, mins)
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

