package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel

@Composable
fun MenuScreen(navController: NavController) {
    val viewModel: AuthenticationViewModel = viewModel()
    val role by viewModel.currentUserRole.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserRole()
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(
                title = "Menu",
                navController = navController,
                showLogout = true,
                onLogout = {
                    viewModel.signOut()
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
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
    val context = LocalContext.current

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
    MenuTable(actions) { index ->
        when (index) {
            0 -> {
                viewModel.signOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
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
    MenuTable(actions)
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
    MenuTable(actions)
}

@Composable
private fun MenuTable(actions: List<String>, onActionSelected: ((Int) -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        actions.forEachIndexed { index, action ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                Text(text = "${index + 1}.", modifier = Modifier.width(24.dp))
                if (onActionSelected != null) {
                    Text(
                        text = action,
                        modifier = Modifier
                            .clickable { onActionSelected(index) }
                            .weight(1f)
                    )
                } else {
                    Text(text = action, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
