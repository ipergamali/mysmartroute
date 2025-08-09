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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

@Composable
fun PrintScheduledScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val requests by requestViewModel.requests.collectAsState()
    val pois by poiViewModel.pois.collectAsState()

    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        requestViewModel.loadRequests(context, allUsers = true)
    }

    val currentDriver = FirebaseAuth.getInstance().currentUser?.uid
    val scheduled = requests.filter { it.status == "accepted" && it.driverId == currentDriver }
    val poiNames = pois.associate { it.id to it.name }

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
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            if (scheduled.isEmpty()) {
                Text(text = stringResource(R.string.no_scheduled_transports))
            } else {
                LazyColumn {
                    items(scheduled) { req ->
                        val fromName = poiNames[req.startPoiId] ?: ""
                        val toName = poiNames[req.endPoiId] ?: ""
                        Text("$fromName → $toName")
                        Divider()
                    }
                }
            }
        }
    }
}
