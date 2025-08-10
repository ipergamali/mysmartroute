package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val role by authViewModel.currentUserRole.collectAsState()
    val requests by requestViewModel.requests.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
    }

    LaunchedEffect(role) {
        role?.let {
            requestViewModel.loadRequests(context, allUsers = it == UserRole.DRIVER)
        }
    }

    LaunchedEffect(role, requests) {
        role?.let {
            requestViewModel.markNotificationsRead(allUsers = it == UserRole.DRIVER)
        }
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val notifications = when (role) {
        UserRole.DRIVER -> requests.filter {
            (it.driverId.isBlank() && it.status.isBlank()) ||
                (it.driverId == userId && (it.status == "accepted" || it.status == "rejected"))
        }
        UserRole.PASSENGER -> requests.filter { it.status == "pending" }
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.notifications),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (notifications.isEmpty()) {
                Text(stringResource(R.string.no_notifications))
            } else {
                LazyColumn {
                    items(notifications) { req ->
                        val message = when (role) {
                            UserRole.DRIVER -> when {
                                req.driverId.isBlank() -> stringResource(
                                    R.string.passenger_request_notification,
                                    req.createdByName,
                                    req.requestNumber
                                )

                                req.status == "accepted" -> stringResource(
                                    R.string.request_accepted_notification,
                                    req.requestNumber
                                )

                                req.status == "rejected" -> stringResource(
                                    R.string.request_rejected_notification,
                                    req.requestNumber
                                )

                                else -> ""
                            }

                            UserRole.PASSENGER -> stringResource(
                                R.string.driver_offer_notification,
                                req.driverName,
                                req.requestNumber
                            )

                            else -> ""
                        }
                        Text(message)
                        Divider()
                    }
                }
            }
        }
    }
}
