package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TransportAnnouncementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController) {
    val viewModel: TransportAnnouncementViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    var startLatLng by remember { mutableStateOf<LatLng?>(null) }
    var endLatLng by remember { mutableStateOf<LatLng?>(null) }
    var costInput by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(0) }
    var dateInput by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.9838, 23.7275), 9f)
    }

    LaunchedEffect(startLatLng, endLatLng) {
        if (startLatLng != null && endLatLng != null) {
            // Replace with your real API key
            val apiKey = context.getString(R.string.google_maps_key)
            durationMinutes = MapsUtils.fetchDuration(startLatLng!!, endLatLng!!, apiKey)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = "Announce Transport", navController = navController)
        Spacer(modifier = Modifier.height(16.dp))

        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                if (startLatLng == null) {
                    startLatLng = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 10f)
                } else if (endLatLng == null) {
                    endLatLng = latLng
                } else {
                    startLatLng = latLng
                    endLatLng = null
                }
            }
        ) {
            startLatLng?.let { Marker(position = it, title = "From") }
            endLatLng?.let { Marker(position = it, title = "To") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = costInput, onValueChange = { costInput = it }, label = { Text("Cost") })
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Duration: $durationMinutes min")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("Date") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val cost = costInput.toDoubleOrNull() ?: 0.0
            val date = dateInput.toIntOrNull() ?: 0
            val start = startLatLng?.let { "${it.latitude},${it.longitude}" } ?: ""
            val end = endLatLng?.let { "${it.latitude},${it.longitude}" } ?: ""
            val route = Route(start, end, cost)
            viewModel.announce(route, VehicleType.CAR, date, cost, durationMinutes)
        }) {
            Text("Announce")
        }

        if (state is TransportAnnouncementViewModel.AnnouncementState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = (state as TransportAnnouncementViewModel.AnnouncementState.Error).message)
        }
    }

    LaunchedEffect(state) {
        if (state is TransportAnnouncementViewModel.AnnouncementState.Success) {
            navController.popBackStack()
        }
    }
}
