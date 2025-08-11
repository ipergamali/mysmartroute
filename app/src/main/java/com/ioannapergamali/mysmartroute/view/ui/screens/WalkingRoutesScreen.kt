package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.data.local.MovingEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingRoutesScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleRequestViewModel = viewModel()
    val context = LocalContext.current
    val movings by viewModel.movings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRequests(context)
    }

    val walkingMovings = movings.filter { it.vehicleId == VehicleRequestViewModel.WALKING_ID }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.walking_routes),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (walkingMovings.isEmpty()) {
                Text(stringResource(R.string.no_walking_routes))
            } else {
                walkingMovings.forEach { m ->
                    WalkingRow(m, onRespond = { accept ->
                        viewModel.setWalkingStatus(context, m.id, accept)
                    })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WalkingRow(m: MovingEntity, onRespond: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(m.routeName)
        Row {
            TextButton(onClick = { onRespond(true) }) {
                Text(stringResource(R.string.accept_offer))
            }
            TextButton(onClick = { onRespond(false) }) {
                Text(stringResource(R.string.reject_offer))
            }
        }
    }
}
