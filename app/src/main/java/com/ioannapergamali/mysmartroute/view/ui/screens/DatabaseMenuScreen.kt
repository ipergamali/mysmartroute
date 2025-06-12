package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
        }
    }
}
