package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.data.local.isAwaitingDriver
import com.ioannapergamali.mysmartroute.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val role by authViewModel.currentUserRole.collectAsState()
    val requests by requestViewModel.requests.collectAsState()
    val userId = SessionManager.currentUserId() ?: ""
    val systemNotifications by userViewModel.getNotifications(context, userId).collectAsState(emptyList())

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

    val requestMessages = when (role) {
        UserRole.DRIVER -> requests.filter {
            it.isAwaitingDriver() ||
                (it.driverId == userId && (it.status == "accepted" || it.status == "rejected"))
        }.map { req ->
            when {
                req.isAwaitingDriver() -> stringResource(
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
        }

        UserRole.PASSENGER -> requests.filter {
            it.status == "pending" && it.driverId.isNotBlank()
        }.map { req ->
            stringResource(
                R.string.driver_offer_notification,
                req.driverName,
                req.requestNumber
            )
        }

        else -> emptyList()
    }

    val storedNotificationItems = systemNotifications.map { notification ->
        NotificationListItem(
            key = notification.id,
            message = notification.message,
            sentDate = notification.sentDate,
            sentTime = notification.sentTime,
            canDelete = true,
            notificationId = notification.id,
            navigateToRequests = false
        )
    }

    val requestNotificationItems = requestMessages.mapIndexed { index, message ->
        NotificationListItem(
            key = "request_$index",
            message = message,
            sentDate = "",
            sentTime = "",
            canDelete = false,
            notificationId = null,
            navigateToRequests = true
        )
    }

    val notificationItems = storedNotificationItems + requestNotificationItems
    val requestScreen = when (role) {
        UserRole.DRIVER -> "viewTransportRequests"
        else -> "viewRequests"
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
            if (notificationItems.isEmpty()) {
                Text(stringResource(R.string.no_notifications))
            } else {
                LazyColumn {
                    items(notificationItems, key = { it.key }) { item ->
                        NotificationRow(
                            item = item,
                            onClick = if (item.navigateToRequests) {
                                { navController.navigate(requestScreen) }
                            } else null,
                            onDelete = item.notificationId?.let { id ->
                                { userViewModel.deleteNotification(context, id) }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

private data class NotificationListItem(
    val key: String,
    val message: String,
    val sentDate: String,
    val sentTime: String,
    val canDelete: Boolean,
    val notificationId: String?,
    val navigateToRequests: Boolean
)

@Composable
private fun NotificationRow(
    item: NotificationListItem,
    onClick: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium
            )
            val timestamp = listOf(
                item.sentDate.takeIf { it.isNotBlank() },
                item.sentTime.takeIf { it.isNotBlank() }
            ).filterNotNull().joinToString(" • ")
            if (timestamp.isNotEmpty()) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (item.canDelete && onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_notification)
                )
            }
        }
    }
}
