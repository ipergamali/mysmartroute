package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar

@Composable
fun DatabaseMenuScreen(navController: NavController, openDrawer: () -> Unit) {
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
            Button(onClick = { navController.navigate("localDb") }) {
                Text("Local DB")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { navController.navigate("firebaseDb") }) {
                Text("Firestore DB")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { navController.navigate("syncDb") }) {
                Text("Synchronization")
            }
        }
    }
}
