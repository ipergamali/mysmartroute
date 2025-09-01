package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.WalkingUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import kotlin.time.Duration

@Composable
fun DefineDurationScreen(navController: NavController, openDrawer: () -> Unit) {
    var distanceText by rememberSaveable { mutableStateOf("") }
    var duration: Duration? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val inputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }

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
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text(stringResource(R.string.distance_meters)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                distanceText.toDoubleOrNull()?.let {
                    duration = WalkingUtils.walkingDuration(it)
                }
            }) {
                Text(stringResource(R.string.calculate))
            }

            duration?.let {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.walking_duration_result, it.toString()))
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.walking_routes))
            Spacer(Modifier.height(8.dp))

            if (routes.isEmpty()) {
                Text(stringResource(R.string.no_walking_routes))
            } else {
                routes.forEach { route ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(route.name, modifier = Modifier.weight(1f))
                        val current = inputs.getOrElse(route.id) {
                            route.walkDurationMinutes.takeIf { it > 0 }?.toString() ?: ""
                        }
                        OutlinedTextField(
                            value = current,
                            onValueChange = { inputs[route.id] = it },
                            label = { Text(stringResource(R.string.duration)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
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

