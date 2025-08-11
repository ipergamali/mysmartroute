package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransferRequestViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus
import android.text.format.DateFormat
import java.util.Date

private enum class SortOption { COST, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRequestsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    initialRequestId: String? = null
) {
    val context = LocalContext.current
    val viewModel: VehicleRequestViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val transferViewModel: TransferRequestViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val driverNames = remember { mutableStateMapOf<String, String>() }
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val columnWidth = 150.dp
    val sortOption = remember { mutableStateOf(SortOption.COST) }
    val sortedRequests = when (sortOption.value) {
        SortOption.COST -> requests.sortedBy { it.cost }
        SortOption.DATE -> requests.sortedBy { it.date }
    }

    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        viewModel.loadRequests(context)
    }

    LaunchedEffect(requests) {
        requests.forEach { req ->
            if (req.driverId.isNotBlank() && driverNames[req.driverId] == null) {
                driverNames[req.driverId] = userViewModel.getUserName(context, req.driverId)
            }
        }
    }

    LaunchedEffect(sortedRequests) {
        initialRequestId?.let { id ->
            val index = sortedRequests.indexOfFirst { it.id == id }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
            }
        }
    }

    val poiNames = pois.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_requests),
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
                Row {
                    IconButton(onClick = { sortOption.value = SortOption.COST }) {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = stringResource(R.string.sort_by_cost)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { sortOption.value = SortOption.DATE }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(R.string.sort_by_date)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    LazyColumn(state = listState) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.request_number),
                                    modifier = Modifier.width(columnWidth)
                                )
                                Text(
                                    stringResource(R.string.route_name),
                                    modifier = Modifier.width(columnWidth)
                                )
                                Text(
                                    stringResource(R.string.cost),
                                    modifier = Modifier.width(columnWidth)
                                )
                                Text(
                                    stringResource(R.string.date),
                                    modifier = Modifier.width(columnWidth)
                                )
                            }
                            Divider()
                        }
                        items(sortedRequests) { req ->
                            val fromName = poiNames[req.startPoiId] ?: ""
                            val toName = poiNames[req.endPoiId] ?: ""
                            val routeName = if (fromName.isNotBlank() && toName.isNotBlank())
                                "$fromName → $toName" else ""
                            val dateTimeText = if (req.date > 0L) {
                                val date = Date(req.date)
                                val dateStr = DateFormat.getDateFormat(context).format(date)
                                val timeStr = DateFormat.format("HH:mm", date).toString()
                                "$dateStr $timeStr"
                            } else ""
                            val costText = if (req.cost == Double.MAX_VALUE) "-" else req.cost.toString()
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(req.requestNumber.toString(), modifier = Modifier.width(columnWidth))
                                Text(routeName, modifier = Modifier.width(columnWidth))
                                Text(costText, modifier = Modifier.width(columnWidth))
                                Text(dateTimeText, modifier = Modifier.width(columnWidth))
                                if (req.status == "pending") {
                                    val dName = driverNames[req.driverId] ?: ""
                                    Text(dName, modifier = Modifier.width(columnWidth))
                                    Button(onClick = {
                                        viewModel.respondToOffer(context, req.id, true)
                                        transferViewModel.updateStatus(
                                            context,
                                            req.requestNumber,
                                            RequestStatus.ACCEPTED
                                        )
                                    }) {
                                        Text(stringResource(R.string.accept_offer))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(onClick = {
                                        viewModel.respondToOffer(context, req.id, false)
                                        transferViewModel.updateStatus(
                                            context,
                                            req.requestNumber,
                                            RequestStatus.REJECTED
                                        )
                                    }) {
                                        Text(stringResource(R.string.reject_offer))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(onClick = {
                                        transferViewModel.updateStatus(
                                            context,
                                            req.requestNumber,
                                            RequestStatus.CANCELED
                                        )
                                        viewModel.deleteRequests(context, setOf(req.id))
                                    }) {
                                        Text(stringResource(R.string.cancel_request))
                                    }
                                } else {
                                    Text(req.status, modifier = Modifier.width(columnWidth))
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

