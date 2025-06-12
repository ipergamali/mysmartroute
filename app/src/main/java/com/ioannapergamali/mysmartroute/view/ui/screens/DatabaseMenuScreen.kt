package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel

@Composable
fun DatabaseMenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(onClick = { navController.navigate("localDb") }) {
                Text("Local DB")
            }
            Button(onClick = { navController.navigate("firebaseDb") }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Firebase")
            }
            Button(onClick = { viewModel.syncDatabases(context) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Synchronization")
            }
        }
    }

    LaunchedEffect(syncState) {
        when (syncState) {
            is DatabaseViewModel.SyncState.Success -> Toast.makeText(context, "Sync completed", Toast.LENGTH_SHORT).show()
            is DatabaseViewModel.SyncState.Error -> Toast.makeText(
                context,
                (syncState as DatabaseViewModel.SyncState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            else -> {}
        }
    }
}
