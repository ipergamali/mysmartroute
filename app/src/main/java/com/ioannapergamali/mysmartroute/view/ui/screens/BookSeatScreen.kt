package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSeatScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: BookingViewModel = viewModel()
    val routes by viewModel.availableRoutes.collectAsState()
    var selectedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.book_seat),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            var expanded by remember { mutableStateOf(false) }

            Box {
                Button(onClick = { expanded = true }) {
                    Text(selectedRoute?.name ?: stringResource(R.string.select_route))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                selectedRoute = route
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = selectedRoute != null,
                onClick = {
                    selectedRoute?.let { r ->
                        val success = viewModel.reserveSeat(r.id)
                        message = if (success) {
                            stringResource(R.string.seat_booked)
                        } else {
                            stringResource(R.string.seat_unavailable)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.reserve_seat))
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
