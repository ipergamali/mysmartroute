package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MovingStatus
import com.ioannapergamali.mysmartroute.data.local.categorizeMovings
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel

private const val TAG = "PassengerMovingsScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMovingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleRequestViewModel = viewModel()
    val movings by viewModel.movings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadPassengerMovings(context)
    }


    Log.d(TAG, "Φόρτωση ${movings.size} μετακινήσεων επιβάτη")
    val grouped = categorizeMovings(movings).also { map ->
        map.forEach { (status, list) ->
            Log.d(TAG, "Κατηγορία $status έχει ${list.size} εγγραφές")

        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_movings),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (movings.isEmpty()) {
                Text(stringResource(R.string.no_movings))
            } else {
                MovingCategory(
                    stringResource(R.string.active_movings),
                    grouped[MovingStatus.ACTIVE].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.pending_movings),
                    grouped[MovingStatus.PENDING].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.unsuccessful_movings),
                    grouped[MovingStatus.UNSUCCESSFUL].orEmpty()
                )
                MovingCategory(
                    stringResource(R.string.completed_movings),
                    grouped[MovingStatus.COMPLETED].orEmpty()
                )
            }
        }
    }
}

@Composable
private fun MovingCategory(title: String, list: List<MovingEntity>) {

    Log.d(TAG, "Εμφάνιση κατηγορίας $title με ${list.size} εγγραφές")

    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    MovingTable(list)
    if (list.isEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.no_movings))
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MovingTable(list: List<MovingEntity>) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scrollState)
    ) {
        Column {
            Row {
                TableHeaderCell(stringResource(R.string.route))
                TableHeaderCell(stringResource(R.string.driver))
                TableHeaderCell(stringResource(R.string.vehicle_name))
                TableHeaderCell(stringResource(R.string.passenger))
            }
            Divider()
            list.forEach { m ->

                Log.d(TAG, "Γραμμή για μετακίνηση ${m.id} με status ${m.status}")

                Row {
                    TableCell(m.routeName.ifBlank { "-" })
                    TableCell(m.driverName.ifBlank { "-" })
                    TableCell(m.vehicleName.ifBlank { "-" })
                    TableCell(m.createdByName.ifBlank { "-" })
                }
                Divider()
            }
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String) {
    Text(
        text,
        modifier = Modifier
            .width(120.dp)
            .padding(4.dp),
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun RowScope.TableCell(text: String) {
    Text(
        text,
        modifier = Modifier
            .width(120.dp)
            .padding(4.dp)
    )
}
