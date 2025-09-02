package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
fun ViewUnassignedScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val inputs = remember { mutableStateMapOf<String, String>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { routeViewModel.loadRoutesWithoutDuration() }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_unassigned),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            val unassigned = routes
            if (unassigned.isEmpty()) {
                Text(stringResource(R.string.no_unassigned_routes))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.route), modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.duration), modifier = Modifier.width(100.dp))
                            Text(stringResource(R.string.calculate))
                            Text(stringResource(R.string.save))
                        }
                    }
                    items(unassigned) { route ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(route.name, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = inputs.getOrElse(route.id) { "" },
                                onValueChange = { inputs[route.id] = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp),
                                placeholder = { Text(stringResource(R.string.duration)) }
                            )
                            Button(onClick = {
                                coroutineScope.launch {
                                    val distance = routeViewModel.getRouteDistance(context, route.id)
                                    val mins =
                                        WalkingUtils.walkingDuration(distance.toDouble()).inWholeMinutes
                                            .toInt()
                                    inputs[route.id] = mins.toString()
                                }
                            }) {
                                Text(stringResource(R.string.calculate))
                            }
                            Button(onClick = {
                                inputs[route.id]?.toIntOrNull()?.let { mins ->
                                    routeViewModel.updateWalkDuration(context, route.id, mins)
                                    inputs.remove(route.id)
                                }
                            }) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}
