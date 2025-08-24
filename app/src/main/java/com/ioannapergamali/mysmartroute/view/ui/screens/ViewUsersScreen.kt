package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.UserStatsViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserSummary

@Composable
fun ViewUsersScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: UserStatsViewModel = viewModel()
    val summaries by viewModel.userSummaries.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_users),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (summaries.isEmpty()) {
                Text(stringResource(R.string.no_users))
            } else {
                val sorted = summaries.sortedBy { it.user.role }
                LazyColumn {
                    var currentRole: UserRole? = null
                    sorted.forEach { summary ->
                        val role = runCatching { UserRole.valueOf(summary.user.role) }.getOrNull()
                        if (role != null && role != currentRole) {
                            currentRole = role
                            item {
                                Text(
                                    role.localizedName(),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            UserSummaryItem(summary)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSummaryItem(summary: UserSummary) {
    val user = summary.user
    Text("${user.name} ${user.surname}", style = MaterialTheme.typography.titleMedium)
    val role = runCatching { UserRole.valueOf(user.role) }.getOrNull()
    if (summary.completedMovings.isNotEmpty()) {
        Text(stringResource(R.string.completed_movings), style = MaterialTheme.typography.labelLarge)
        summary.completedMovings.forEach { m ->
            Text("- ${m.routeName}")
        }
    } else {
        Text(stringResource(R.string.no_completed_movings))
    }
    Text(stringResource(R.string.total_cost_label, summary.totalCost))
    Text(stringResource(R.string.average_rating_label, summary.passengerAverageRating))
    if (role == UserRole.DRIVER || role == UserRole.ADMIN) {
        if (summary.vehicles.isNotEmpty()) {
            Text(stringResource(R.string.vehicles_label), style = MaterialTheme.typography.labelLarge)
            summary.vehicles.forEach { v -> Text("- ${v.name}") }
        }
        Text(stringResource(R.string.passenger_avg_rating_label, summary.driverAverageRating))
    }
}

