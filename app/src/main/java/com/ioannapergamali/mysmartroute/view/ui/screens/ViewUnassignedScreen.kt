package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (routes.isEmpty()) {
                Text(stringResource(R.string.no_unassigned_routes))
            } else {
                routes.forEach { route ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(route.name, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = inputs.getOrElse(route.id) { "" },
                            onValueChange = { inputs[route.id] = it },
                            label = { Text(stringResource(R.string.duration)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
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
                            }
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
