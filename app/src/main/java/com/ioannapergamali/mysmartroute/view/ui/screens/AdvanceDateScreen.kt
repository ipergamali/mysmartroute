@file:OptIn(ExperimentalMaterial3Api::class)
package com.ioannapergamali.mysmartroute.view.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AppDateTimeViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.ioannapergamali.mysmartroute.utils.ATHENS_TIME_ZONE

@Composable
fun AdvanceDateScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val dateViewModel: AppDateTimeViewModel = viewModel()
    val storedMillis by dateViewModel.dateTime.collectAsState()
    val format = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
            timeZone = ATHENS_TIME_ZONE
        }
    }

    LaunchedEffect(Unit) { dateViewModel.load(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.advance_date),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            val systemText = format.format(Date())
            val storedText = storedMillis?.let { format.format(Date(it)) } ?: ""
            Text(stringResource(R.string.system_datetime) + ": " + systemText)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.stored_datetime) + ": " + storedText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val cal = Calendar.getInstance(ATHENS_TIME_ZONE)
                DatePickerDialog(context, { _, y, m, d ->
                    TimePickerDialog(context, { _, h, min ->
                        val newCal = Calendar.getInstance(ATHENS_TIME_ZONE).apply { set(y, m, d, h, min) }
                        dateViewModel.save(context, newCal.timeInMillis)
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text(stringResource(R.string.edit)) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { dateViewModel.reset(context) }) {
                Text(stringResource(R.string.reset_time))
            }
        }
    }
}
