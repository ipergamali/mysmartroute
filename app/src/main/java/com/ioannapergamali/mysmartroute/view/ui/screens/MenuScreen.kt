package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.movewise.ui.components.TopBar
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel

@Composable
fun MenuScreen(navController: NavController) {
    val viewModel: AuthenticationViewModel = viewModel()
    val role by viewModel.currentUserRole.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserRole()
    }

    Scaffold(
        topBar = {
            TopBar(title = "Menu", navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (role) {
                UserRole.PASSENGER -> PassengerMenu(viewModel, navController)
                UserRole.DRIVER -> DriverMenu()
                UserRole.ADMIN -> AdminMenu()
                null -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun PassengerMenu(viewModel: AuthenticationViewModel, navController: NavController) {
    val actions = listOf(
        "Sign out",
        "Manage Favorite Means of Transport",
        "Μode Of Transportation For A Specific Route",
        "Find a Vehicle for a specific Transport",
        "Find Way of Transport",
        "Book a Seat or Buy a Ticket",
        "View Interesting Routes",
        "View Transports",
        "Print Booked Seat or Ticket",
        "Cancel Booked Seat",
        "View, Rank and Comment on Completed Transports",
        "Shut Down the System"
    )
    PassengerActionList(actions) { index ->
        when (index) {
            0 -> {
                viewModel.signOut()
                navController.navigate("home") {
                    popUpTo("menu") { inclusive = true }
                }
            }
            // TODO: handle other passenger actions
        }
    }
}

@Composable
private fun DriverMenu() {
    val actions = listOf(
        "Register Vehicle",
        "Announce Availability for a specific Transport",
        "Find Passengers",
        "Print Passenger List",
        "Print Passenger List for Scheduled Transports",
        "Print Passenger List for Completed Transports"
    )
    ActionList(actions)
}

@Composable
private fun AdminMenu() {
    val actions = listOf(
        "Initialize System",
        "Create User Account",
        "Promote or Demote User",
        "Define Point of Interest",
        "Define Duration of Travel by Foot",
        "View List of Unassigned Routes",
        "Review Point of Interest Names",
        "Show 10 Best and Worst Drivers",
        "View 10 Happiest/Least Happy Passengers",
        "View Available Vehicles",
        "View PoIs",
        "View Users",
        "Advance Date"
    )
    ActionList(actions)
}

@Composable
private fun ActionList(actions: List<String>) {
    actions.forEachIndexed { index, action ->
        Text(text = "${index + 1}. $action", modifier = Modifier.padding(4.dp))
    }
}

@Composable
private fun PassengerActionList(actions: List<String>, onActionSelected: (Int) -> Unit) {
    actions.forEachIndexed { index, action ->
        Button(
            onClick = { onActionSelected(index) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(text = "${index + 1}. $action")
        }
    }
}
