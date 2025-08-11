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
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrintScheduledScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val declarations by declarationViewModel.pendingDeclarations.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val passengerNames = remember { mutableStateMapOf<String, List<String>>() }

    LaunchedEffect(Unit) {
        val driverId = FirebaseAuth.getInstance().currentUser?.uid
        if (driverId != null) {
            declarationViewModel.loadDeclarations(context, driverId)
            routeViewModel.loadRoutes(context)
        }
    }

    val routeNames = routes.associate { it.id to it.name }
    val scheduled = declarations.filter { it.date >= System.currentTimeMillis() }

    LaunchedEffect(scheduled) {
        val db = MySmartRouteDatabase.getInstance(context)
        scheduled.forEach { decl ->
            if (passengerNames[decl.id] == null) {
                val reservations = db.seatReservationDao().getReservationsForDeclaration(decl.id).first()
                val names = reservations.map { res ->
                    userViewModel.getUserName(context, res.userId)
                }.filter { it.isNotBlank() }
                passengerNames[decl.id] = names
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.print_scheduled),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (scheduled.isEmpty()) {
                Text(text = stringResource(R.string.no_scheduled_transports))
            } else {
                val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                LazyColumn {
                    items(scheduled) { decl ->
                        val routeName = routeNames[decl.routeId] ?: ""
                        val dateText = formatter.format(Date(decl.date))
                        val passengers = passengerNames[decl.id].orEmpty()
                        Text("$routeName – $dateText")
                        passengers.forEach { name ->
                            Text("- $name")
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
