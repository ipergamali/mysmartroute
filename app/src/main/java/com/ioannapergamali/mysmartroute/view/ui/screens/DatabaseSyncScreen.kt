package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseSyncScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadLastSync(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Synchronization",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Button(onClick = { viewModel.syncDatabases(context) }) {
                Text("Sync Now")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = "Τελευταίος συγχρονισμός: " +
                    (if (lastSync == 0L) "-" else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastSync)))
            )
            if (syncState is SyncState.Loading) {
                Spacer(modifier = Modifier.padding(8.dp))
                CircularProgressIndicator()
            }
        }
    }
}
