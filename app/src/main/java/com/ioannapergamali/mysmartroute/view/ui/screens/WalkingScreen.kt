package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleRequestViewModel = viewModel()
    val context = LocalContext.current
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.walking),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text(stringResource(R.string.date)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it },
                label = { Text(stringResource(R.string.time)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val timestamp = runCatching { formatter.parse("$dateText $timeText")?.time }
                    .getOrNull() ?: System.currentTimeMillis()
                viewModel.logWalking(context, timestamp)
            }) {
                Icon(Icons.Filled.DirectionsWalk, contentDescription = stringResource(R.string.walking))
            }
        }
    }
}
