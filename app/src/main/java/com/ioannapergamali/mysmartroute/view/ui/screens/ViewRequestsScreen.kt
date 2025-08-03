package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import android.text.format.DateFormat
import java.util.Date

private enum class SortOption { COST, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRequestsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VehicleRequestViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val scrollState = rememberScrollState()
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
                    Button(onClick = { sortOption.value = SortOption.COST }) {
                        Text(stringResource(R.string.sort_by_cost))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { sortOption.value = SortOption.DATE }) {
                        Text(stringResource(R.string.sort_by_date))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    LazyColumn {
                        items(sortedRequests) { req ->
                            val fromName = poiNames[req.startPoiId] ?: ""
                            val toName = poiNames[req.endPoiId] ?: ""

                                Text(fromName, modifier = Modifier.width(columnWidth))
                                Text(toName, modifier = Modifier.width(columnWidth))
                                val costText = if (req.cost == Double.MAX_VALUE) "∞" else req.cost.toString()
                                Text(costText, modifier = Modifier.width(columnWidth))
                                Text(dateText, modifier = Modifier.width(columnWidth))
                                Button(onClick = { viewModel.deleteRequests(context, setOf(req.id)) }) {
                                    Text(stringResource(R.string.cancel_request))
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
