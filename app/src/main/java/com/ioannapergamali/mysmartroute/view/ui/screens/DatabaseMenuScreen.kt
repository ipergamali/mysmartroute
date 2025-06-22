package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.SyncState

@Composable
fun DatabaseMenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current

    val localData by viewModel.localData.collectAsState()
    val firebaseData by viewModel.firebaseData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLocalData(context)
        viewModel.loadFirebaseData()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Databases",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            DatabaseDataSection(title = "Local DB", data = localData)
            Spacer(modifier = Modifier.padding(8.dp))
            DatabaseDataSection(title = "Firebase DB", data = firebaseData)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { viewModel.syncDatabases(context) }) {
                Text("Synchronization")
            }
        }
    }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> Toast.makeText(context, "Sync completed", Toast.LENGTH_SHORT).show()
            is SyncState.Error -> Toast.makeText(
                context,
                (syncState as SyncState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            else -> {}
        }
    }
}

@Composable
private fun DatabaseDataSection(title: String, data: com.ioannapergamali.mysmartroute.viewmodel.DatabaseData?) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    if (data == null) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    } else {
        DatabaseDataList(data)
    }
}

@Composable
private fun DatabaseDataList(data: com.ioannapergamali.mysmartroute.viewmodel.DatabaseData) {
    Text("Users", style = MaterialTheme.typography.titleMedium)
    if (data.users.isEmpty()) {
        Text("Ο πίνακας είναι άδειος")
    } else {
        data.users.forEach { user ->
            Text("${'$'}{user.id} - ${'$'}{user.username}")
        }
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text("Vehicles", style = MaterialTheme.typography.titleMedium)
    if (data.vehicles.isEmpty()) {
        Text("Ο πίνακας είναι άδειος")
    } else {
        data.vehicles.forEach { vehicle ->
            Text("${'$'}{vehicle.id} - ${'$'}{vehicle.description}")
        }
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text("PoIs", style = MaterialTheme.typography.titleMedium)
    if (data.pois.isEmpty()) {
        Text("Ο πίνακας είναι άδειος")
    } else {
        data.pois.forEach { poi ->
            Text("${'$'}{poi.id} - ${'$'}{poi.name}")
        }
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text("Settings", style = MaterialTheme.typography.titleMedium)
    if (data.settings.isEmpty()) {
        Text("Ο πίνακας είναι άδειος")
    } else {
        data.settings.forEach { setting ->
            Text("${'$'}{setting.userId} - ${'$'}{setting.theme}")
        }
    }
}
