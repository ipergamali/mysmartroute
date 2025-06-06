package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.movewise.ui.components.TopBar
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

@Composable
fun MenuScreen(navController: NavController, role: UserRole) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val title = when (role) {
        UserRole.DRIVER -> "Driver Menu"
        UserRole.PASSENGER -> "Passenger Menu"
        UserRole.ADMIN -> "Admin Menu"
    }

    data class MenuItem(val id: Int, val label: String, val action: () -> Unit)

    val items = when (role) {
        UserRole.PASSENGER -> listOf(
            MenuItem(0, "Sign out") {
                auth.signOut()
                navController.navigate("login") { popUpTo("home") { inclusive = false } }
            },
            MenuItem(1, "Manage Favorite Means of Transport") {
                Toast.makeText(context, "Manage Favorites", Toast.LENGTH_SHORT).show()
            },
            MenuItem(2, "Mode Of Transportation For A Specific Route") {
                Toast.makeText(context, "Specify Transportation Mode", Toast.LENGTH_SHORT).show()
            },
            MenuItem(3, "Find a Vehicle for a specific Transport") {
                Toast.makeText(context, "Finding Vehicle", Toast.LENGTH_SHORT).show()
            },
            MenuItem(4, "Find Way of Transport") {
                Toast.makeText(context, "Finding Way", Toast.LENGTH_SHORT).show()
            },
            MenuItem(5, "Book a Seat or Buy a Ticket") {
                Toast.makeText(context, "Booking...", Toast.LENGTH_SHORT).show()
            },
            MenuItem(6, "View Interesting Routes") {
                Toast.makeText(context, "Interesting Routes", Toast.LENGTH_SHORT).show()
            },
            MenuItem(7, "View Transports") {
                Toast.makeText(context, "Viewing Transports", Toast.LENGTH_SHORT).show()
            },
            MenuItem(8, "Print Booked Seat or Ticket") {
                Toast.makeText(context, "Printing Ticket", Toast.LENGTH_SHORT).show()
            },
            MenuItem(9, "Cancel Booked Seat") {
                Toast.makeText(context, "Canceling Seat", Toast.LENGTH_SHORT).show()
            },
            MenuItem(10, "View, Rank and Comment on Completed Transports") {
                Toast.makeText(context, "Viewing Completed Transports", Toast.LENGTH_SHORT).show()
            },
            MenuItem(11, "Shut Down the System") {
                Toast.makeText(context, "Exiting...", Toast.LENGTH_SHORT).show()
            }
        )

        UserRole.DRIVER -> listOf(
            MenuItem(1, "Register Vehicle") {
                Toast.makeText(context, "Register Vehicle", Toast.LENGTH_SHORT).show()
            },
            MenuItem(2, "Announce Availability for a specific Transport") {
                Toast.makeText(context, "Announce Availability", Toast.LENGTH_SHORT).show()
            },
            MenuItem(3, "Find Passengers") {
                Toast.makeText(context, "Finding Passengers", Toast.LENGTH_SHORT).show()
            },
            MenuItem(4, "Print Passenger List") {
                Toast.makeText(context, "Passenger List", Toast.LENGTH_SHORT).show()
            },
            MenuItem(5, "Print Passenger List for Scheduled Transports") {
                Toast.makeText(context, "Scheduled Passenger List", Toast.LENGTH_SHORT).show()
            },
            MenuItem(6, "Print Passenger List for Completed Transports") {
                Toast.makeText(context, "Completed Passenger List", Toast.LENGTH_SHORT).show()
            }
        )

        UserRole.ADMIN -> listOf(
            MenuItem(1, "Initialize System") {
                Toast.makeText(context, "Initializing System", Toast.LENGTH_SHORT).show()
            },
            MenuItem(2, "Create User Account") {
                Toast.makeText(context, "Create User", Toast.LENGTH_SHORT).show()
            },
            MenuItem(3, "Promote or Demote User") {
                Toast.makeText(context, "Edit User Privilege", Toast.LENGTH_SHORT).show()
            },
            MenuItem(4, "Define Point of Interest") {
                Toast.makeText(context, "Define POI", Toast.LENGTH_SHORT).show()
            },
            MenuItem(5, "Define Duration of Travel by Foot") {
                Toast.makeText(context, "Define Duration", Toast.LENGTH_SHORT).show()
            },
            MenuItem(6, "View List of Unassigned Routes") {
                Toast.makeText(context, "Unassigned Routes", Toast.LENGTH_SHORT).show()
            },
            MenuItem(7, "Review Point of Interest Names") {
                Toast.makeText(context, "Review POIs", Toast.LENGTH_SHORT).show()
            },
            MenuItem(8, "Show 10 Best and Worst Drivers") {
                Toast.makeText(context, "Driver Rankings", Toast.LENGTH_SHORT).show()
            },
            MenuItem(9, "View 10 Happiest/Least Happy Passengers") {
                Toast.makeText(context, "Passenger Mood", Toast.LENGTH_SHORT).show()
            },
            MenuItem(10, "View Available Vehicles") {
                Toast.makeText(context, "Available Vehicles", Toast.LENGTH_SHORT).show()
            },
            MenuItem(11, "View PoIs") {
                Toast.makeText(context, "Viewing POIs", Toast.LENGTH_SHORT).show()
            },
            MenuItem(12, "View Users") {
                Toast.makeText(context, "Viewing Users", Toast.LENGTH_SHORT).show()
            },
            MenuItem(13, "Advance Date") {
                Toast.makeText(context, "Advancing Date", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = { TopBar(title = title, navController = navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items) { item ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = item.action
                ) {
                    Text(text = "${item.id}. ${item.label}")
                }
            }
        }
    }
}
