package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
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

    LaunchedEffect(systemNotifications) {
        if (systemNotifications.isNotEmpty()) {
            userViewModel.markNotificationsRead(context, userId)
        }
    }

    val systemItems = systemNotifications.map { notification ->
        NotificationItem(
            id = notification.id,
            message = notification.message,
            actionRoute = notification.actionRoute.takeIf { it.isNotBlank() },
            highlightTransfer = notification.actionRoute == "viewTransportRequests"
        )
    }

    val requestNotifications = when (role) {
        UserRole.DRIVER -> requests.filter {
            (it.driverId.isBlank() && it.status.isBlank()) ||
                (it.driverId == userId && (it.status == "accepted" || it.status == "rejected"))
        }.mapNotNull { req ->
            when {
                req.driverId.isBlank() -> NotificationItem(
                    id = "driver-open-${req.id}",
                    message = stringResource(
                        R.string.passenger_request_notification,
                        req.createdByName,
                        req.requestNumber
                    ),
                    actionRoute = "viewTransportRequests",
                    highlightTransfer = true
                )

                req.status == "accepted" -> NotificationItem(
                    id = "driver-accepted-${req.id}",
                    message = stringResource(
                        R.string.request_accepted_notification,
                        req.requestNumber
                    ),
                    actionRoute = "viewTransportRequests"
                )

                req.status == "rejected" -> NotificationItem(
                    id = "driver-rejected-${req.id}",
                    message = stringResource(
                        R.string.request_rejected_notification,
                        req.requestNumber
                    ),
                    actionRoute = "viewTransportRequests"
                )

                else -> null
            }
        }

        UserRole.PASSENGER -> requests.filter {
            it.status == "pending" && it.driverId.isNotBlank()
        }.map { req ->
            NotificationItem(
                id = "passenger-pending-${req.id}",
                message = stringResource(
                    R.string.driver_offer_notification,
                    req.driverName,
                    req.requestNumber
                ),
                actionRoute = "viewRequests"
            )
        }

        else -> emptyList()
    }

    val notifications = systemItems + requestNotifications
    val defaultRoute = when (role) {
        UserRole.DRIVER -> "viewTransportRequests"
        UserRole.PASSENGER -> "viewRequests"
        else -> null
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
            val transferKeyword = stringResource(R.string.transfer_keyword)
            if (notifications.isEmpty()) {
                Text(stringResource(R.string.no_notifications))
            } else {
                LazyColumn {
                    items(notifications, key = { it.id }) { notification ->
                        val route = notification.actionRoute ?: defaultRoute
                        val message = notification.message
                        if (route != null) {
                            if (notification.highlightTransfer) {
                                val keywordBounds = findTransferKeywordBounds(message, transferKeyword)
                                val annotated = buildAnnotatedString {
                                    append(message)
                                    addStringAnnotation("route", route, 0, message.length)
                                    if (keywordBounds != null) {
                                        val (start, end) = keywordBounds
                                        addStringAnnotation("keyword", route, start, end)
                                        addStyle(
                                            SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline
                                            ),
                                            start,
                                            end
                                        )
                                    }
                                }
                                ClickableText(
                                    text = annotated,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { offset ->
                                        val keywordRoute = annotated
                                            .getStringAnnotations("keyword", offset, offset)
                                            .firstOrNull()
                                            ?.item
                                        val destination = keywordRoute
                                            ?: annotated
                                                .getStringAnnotations("route", offset, offset)
                                                .firstOrNull()
                                                ?.item
                                        destination?.let { navController.navigate(it) }
                                    }
                                )
                            } else {
                                Text(
                                    message,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate(route) }
                                )
                            }
                        } else {
                            Text(message, modifier = Modifier.fillMaxWidth())
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

private data class NotificationItem(
    val id: String,
    val message: String,
    val actionRoute: String?,
    val highlightTransfer: Boolean = false
)

private fun findTransferKeywordBounds(message: String, keyword: String): Pair<Int, Int>? {
    val normalizedKeyword = keyword.trim()
    if (normalizedKeyword.isEmpty()) return null
    val primaryIndex = message.indexOf(normalizedKeyword, ignoreCase = true)
    if (primaryIndex >= 0) {
        return primaryIndex to primaryIndex + normalizedKeyword.length
    }
    val fallbackKeywords = listOf("μεταφορά", "transfer")
    for (candidate in fallbackKeywords) {
        val index = message.indexOf(candidate, ignoreCase = true)
        if (index >= 0) {
            return index to index + candidate.length
        }
    }
    return null
}
