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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TransportAnnouncementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnounceTransportScreen(navController: NavController) {
    val viewModel: TransportAnnouncementViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = "Announce Transport", navController = navController)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = start, onValueChange = { start = it }, label = { Text("From") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = end, onValueChange = { end = it }, label = { Text("To") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = costInput, onValueChange = { costInput = it }, label = { Text("Cost") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = durationInput, onValueChange = { durationInput = it }, label = { Text("Duration (min)") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("Date") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val cost = costInput.toDoubleOrNull() ?: 0.0
            val duration = durationInput.toIntOrNull() ?: 0
            val date = dateInput.toIntOrNull() ?: 0
            val route = Route(start, end, cost)
            viewModel.announce(route, VehicleType.CAR, date, cost, duration)
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
