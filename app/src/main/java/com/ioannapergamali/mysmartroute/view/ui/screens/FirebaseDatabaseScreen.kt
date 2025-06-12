package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel

@Composable
fun FirebaseDatabaseScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val data by viewModel.firebaseData.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadFirebaseData() }

    Scaffold(
        topBar = {
            TopBar(
                title = "Firebase DB",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        if (data == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item { Text("Users", style = MaterialTheme.typography.titleMedium) }
                items(data!!.users) { user ->
                    Text("${'$'}{user.id} - ${'$'}{user.username}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Vehicles", style = MaterialTheme.typography.titleMedium) }
                items(data!!.vehicles) { vehicle ->
                    Text("${'$'}{vehicle.id} - ${'$'}{vehicle.description}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("PoIs", style = MaterialTheme.typography.titleMedium) }
                items(data!!.pois) { poi ->
                    Text("${'$'}{poi.id} - ${'$'}{poi.name}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Settings", style = MaterialTheme.typography.titleMedium) }
                items(data!!.settings) { setting ->
                    Text("${'$'}{setting.userId} - ${'$'}{setting.theme}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Authentication table δεν είναι διαθέσιμη από το client", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
