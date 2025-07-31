package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewTransportRequestsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VehicleRequestViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        viewModel.loadRequests(context, allUsers = true)
    }

    LaunchedEffect(requests) {
        requests.forEach { req ->
            if (req.userId.isNotBlank() && userNames[req.userId] == null) {
                userNames[req.userId] = userViewModel.getUserName(context, req.userId)
            }
        }
    }

    val poiNames = pois.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_transport_requests),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (requests.isEmpty()) {
                Text(stringResource(R.string.no_requests))
            } else {
                LazyColumn {
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.passenger),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                stringResource(R.string.route_name),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                stringResource(R.string.cost),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Divider()
                    }
                    items(requests) { req ->
                        val fromName = poiNames[req.startPoiId] ?: ""
                        val toName = poiNames[req.endPoiId] ?: ""
                        val routeName = if (fromName.isNotBlank() && toName.isNotBlank()) "$fromName - $toName" else ""
                        val userName = userNames[req.userId] ?: ""
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(userName, modifier = Modifier.weight(1f))
                            Text(routeName, modifier = Modifier.weight(1f))
                            val costText = if (req.cost == Double.MAX_VALUE) "∞" else req.cost.toString()
                            Text(costText, modifier = Modifier.weight(1f))
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
