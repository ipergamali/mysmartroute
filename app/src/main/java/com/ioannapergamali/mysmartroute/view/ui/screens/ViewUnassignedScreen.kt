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
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel

@Composable
fun ViewUnassignedScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val inputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) { routeViewModel.loadRoutes(context, includeAll = true) }

    val unassigned = routes.filter { it.walkDurationMinutes == 0 }

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
            if (unassigned.isEmpty()) {
                Text(stringResource(R.string.no_unassigned_routes))
            } else {
                unassigned.forEach { route ->
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
