package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.PolyUtil
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.WalkingRouteEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.WalkingRoutesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingRoutesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: WalkingRoutesViewModel = viewModel(factory = WalkingRoutesViewModel.Factory(context))
    val routes by viewModel.routes.collectAsState()
    var selected by remember { mutableStateOf<WalkingRouteEntity?>(null) }
    val cameraState = rememberCameraPositionState()

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.walking_routes),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.weight(1f),
                    cameraPositionState = cameraState
                ) {
                    selected?.let { route ->
                        val points = PolyUtil.decode(route.polyline)
                        if (points.isNotEmpty()) {
                            Polyline(points = points)
                            Marker(state = MarkerState(points.first()))
                        }
                    }
                }
                if (routes.isEmpty()) {
                    Text(stringResource(R.string.no_walking_routes), modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(Modifier.fillMaxWidth().height(200.dp)) {
                        items(routes) { route ->
                            Text(
                                route.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = route
                                        val points = PolyUtil.decode(route.polyline)
                                        if (points.isNotEmpty()) {
                                            cameraState.move(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
                                        }
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

