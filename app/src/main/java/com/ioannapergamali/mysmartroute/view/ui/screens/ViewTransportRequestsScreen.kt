package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransferRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AppDateTimeViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.utils.SessionManager
import android.text.format.DateFormat
import android.net.Uri
import java.util.Date
import java.util.Locale
import com.ioannapergamali.mysmartroute.utils.ATHENS_TIME_ZONE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewTransportRequestsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    initialRequestId: String? = null
) {
    val context = LocalContext.current
    val viewModel: VehicleRequestViewModel = viewModel()
    val transferViewModel: TransferRequestViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val dateViewModel: AppDateTimeViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val userNames = remember { mutableStateMapOf<String, String>() }
    val selectedRequests = remember { mutableStateMapOf<String, Boolean>() }
    val dateFormatter = remember(context) {
        DateFormat.getDateFormat(context).apply { timeZone = ATHENS_TIME_ZONE }
    }
    val timeFormatter = remember(context) {
        DateFormat.getTimeFormat(context).apply { timeZone = ATHENS_TIME_ZONE }
    }
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val columnWidth = 150.dp
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val declarations by declarationViewModel.declarations.collectAsState()
    val offlineDriverId by SessionManager.userIdFlow.collectAsState()
    val driverId = offlineDriverId ?: SessionManager.currentUserId()

    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        viewModel.loadRequests(context, allUsers = true)
    }

    LaunchedEffect(driverId) {
        driverId?.let { declarationViewModel.loadDeclarations(context, it) }
    }

    LaunchedEffect(requests) {
        requests.forEach { req ->
            if (req.userId.isNotBlank() && userNames[req.userId] == null) {
                userNames[req.userId] = userViewModel.getUserName(context, req.userId)
            }
        }
        initialRequestId?.let { id ->
            val index = requests.indexOfFirst { it.id == id }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
            }
        }
    }

    val poiNames = pois.associate { it.id to it.name }
    val appTime by dateViewModel.dateTime.collectAsState()
    LaunchedEffect(Unit) { dateViewModel.load(context) }
    val now = appTime ?: System.currentTimeMillis()
    val declaredRouteIds = remember(declarations, driverId) {
        if (driverId != null) {
            declarations.filter { it.driverId == driverId }.map { it.routeId }.toSet()
        } else {
            emptySet()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_transport_requests),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (requests.isEmpty()) {
                Text(stringResource(R.string.no_requests))
            } else {
                val hasSelection = selectedRequests.values.any { it }
                val allSelected = requests.isNotEmpty() && requests.all { selectedRequests[it.id] == true }
                Button(
                    onClick = {
                        val ids = selectedRequests.filterValues { it }.keys
                        viewModel.deleteRequests(context, ids.toSet())
                        ids.forEach { selectedRequests.remove(it) }
                    },
                    enabled = hasSelection
                ) {
                    Text(stringResource(R.string.delete_selected))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    LazyColumn(state = listState) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            requests.forEach { selectedRequests[it.id] = true }
                                        } else {
                                            selectedRequests.clear()
                                        }
                                    },
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(
                                    stringResource(R.string.passenger),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.route_name),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.stops_header),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.cost),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.date),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.time),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.request_number),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.transport_request_new_column),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.transport_request_declaration_link),
                                    modifier = Modifier.width(columnWidth),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Divider()
                        }
                        items(requests) { req ->
                            val fromName = poiNames[req.startPoiId] ?: ""
                            val toName = poiNames[req.endPoiId] ?: ""
                            val stationsText = if (fromName.isNotBlank() && toName.isNotBlank()) "$fromName - $toName" else ""
                            val routeName = req.routeName.ifBlank { stationsText }
                            val userName = userNames[req.userId] ?: ""
                            val isChecked = selectedRequests[req.id] ?: false
                            val dateValue = req.date.takeIf { it > 0L }?.let { Date(it) }
                            val dateText = dateValue?.let { dateFormatter.format(it) } ?: ""
                            val timeText = dateValue?.let { timeFormatter.format(it) } ?: ""
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedRequests[req.id] = true
                                        } else {
                                            selectedRequests.remove(req.id)
                                        }
                                    },
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(userName, modifier = Modifier.width(columnWidth))
                                Text(routeName, modifier = Modifier.width(columnWidth))
                                Text(stationsText, modifier = Modifier.width(columnWidth))
                                val costText = req.cost?.let { costValue ->
                                    String.format(Locale.getDefault(), "%.2f€", costValue)
                                } ?: "-"
                                Text(costText, modifier = Modifier.width(columnWidth))
                                Text(dateText, modifier = Modifier.width(columnWidth))
                                Text(timeText, modifier = Modifier.width(columnWidth))
                                Text(req.requestNumber.toString(), modifier = Modifier.width(columnWidth))
                                val isNewRequest = req.routeId.isBlank() || req.routeId !in declaredRouteIds
                                Text(
                                    text = stringResource(
                                        if (isNewRequest) {
                                            R.string.transport_request_new_yes
                                        } else {
                                            R.string.transport_request_new_no
                                        }
                                    ),
                                    modifier = Modifier.width(columnWidth)
                                )
                                TextButton(
                                    onClick = {
                                        if (req.routeId.isNotBlank()) {
                                            val encodedRouteId = Uri.encode(req.routeId)
                                            val encodedRouteName = Uri.encode(routeName)
                                            val dateParam = req.date.takeIf { it > 0L }?.toString().orEmpty()
                                            val targetRoute =
                                                "announceAvailability?routeId=$encodedRouteId&dateMillis=$dateParam&routeName=$encodedRouteName"
                                            navController.navigate(targetRoute)
                                        }
                                    },
                                    modifier = Modifier.width(columnWidth),
                                    enabled = req.routeId.isNotBlank()
                                ) {
                                    Text(stringResource(R.string.transport_request_declaration_link))
                                }
                                val isExpired = req.date > 0L && now > req.date && req.status != "completed"
                                if (req.status == "open" && !isExpired) {
                                    val canNotifyRoute = !isNewRequest && req.routeId.isNotBlank()
                                    Button(
                                        onClick = {
                                            if (canNotifyRoute) {
                                                viewModel.notifyRoute(context, req.id)
                                                transferViewModel.notifyDriver(context, req.requestNumber)
                                            }
                                        },
                                        modifier = Modifier.width(columnWidth),
                                        enabled = canNotifyRoute
                                    ) {
                                        Text(stringResource(R.string.notify_route))
                                    }
                                } else {
                                    val statusText = if (isExpired) stringResource(R.string.request_unsuccessful) else req.status
                                    Text(statusText, modifier = Modifier.width(columnWidth))
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
