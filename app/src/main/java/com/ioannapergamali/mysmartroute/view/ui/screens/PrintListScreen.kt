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
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import kotlinx.coroutines.flow.first

@Composable
fun PrintListScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val passengerNames = remember { mutableStateMapOf<String, List<String>>() }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context)
    }

    LaunchedEffect(routes) {
        val db = MySmartRouteDatabase.getInstance(context)
        routes.forEach { route ->
            if (passengerNames[route.id] == null) {
                val reservations = db.seatReservationDao().getReservationsForRoute(route.id).first()
                val names = reservations.map { res ->
                    userViewModel.getUserName(context, res.userId)
                }.filter { it.isNotBlank() }.distinct()
                passengerNames[route.id] = names
            }
        }
    }

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
            if (routes.isEmpty()) {
                Text(text = stringResource(R.string.no_passenger_routes))
            } else {
                LazyColumn {
                    items(routes) { route ->
                        Text(route.name)
                        val passengers = passengerNames[route.id].orEmpty()
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

