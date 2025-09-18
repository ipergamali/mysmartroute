package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

@Composable
fun PrintListScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val movings by requestViewModel.movings.collectAsState()
    val passengerNames = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        requestViewModel.loadPassengerMovings(context, allUsers = true)
    }

    LaunchedEffect(movings) {
        movings.forEach { moving ->
            val passengerId = moving.userId
            if (passengerId.isNotBlank() && passengerNames[passengerId].isNullOrBlank()) {
                val cachedName = moving.createdByName.takeIf { it.isNotBlank() }
                    ?: userViewModel.getUserName(context, passengerId)
                if (cachedName.isNotBlank()) {
                    passengerNames[passengerId] = cachedName
                }
            }
        }
    }

    val groupedRoutes = remember(movings) { movings.groupBy { it.routeId } }
    val routeEntries = remember(groupedRoutes) { groupedRoutes.entries.toList() }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.print_list),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (routeEntries.isEmpty()) {
                Text(text = stringResource(R.string.no_passenger_routes))
            } else {
                LazyColumn {
                    items(routeEntries, key = { it.key }) { entry ->
                        val routeMovings = entry.value
                        val firstMoving = routeMovings.firstOrNull()
                        val routeLabel = firstMoving?.routeName?.takeIf { it.isNotBlank() }
                            ?: entry.key.ifBlank { stringResource(R.string.route) }
                        Text(routeLabel)
                        val passengers = routeMovings.map { moving ->
                            val passengerId = moving.userId
                            when {
                                passengerId.isNotBlank() -> {
                                    passengerNames[passengerId]?.takeIf { it.isNotBlank() }
                                        ?: moving.createdByName.takeIf { it.isNotBlank() }
                                        ?: passengerId
                                }
                                moving.createdByName.isNotBlank() -> moving.createdByName
                                else -> stringResource(R.string.passenger)
                            }
                        }.distinct()
                        if (passengers.isEmpty()) {
                            Text(text = stringResource(R.string.no_reservations))
                        } else {
                            passengers.forEach { name ->
                                Text("- $name")
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

