package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.movewise.ui.components.TopBar
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

@Composable
fun MenuScreen(navController: NavController, role: UserRole) {
    val title = when (role) {
        UserRole.DRIVER -> "Driver Menu"
        UserRole.PASSENGER -> "Passenger Menu"
        UserRole.ADMIN -> "Admin Menu"
    }

    Scaffold(
        topBar = { TopBar(title = title, navController = navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to $title")
        }
    }
}
