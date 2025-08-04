package com.ioannapergamali.mysmartroute.view.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReservationDetailsScreen(
    navController: NavController,
    reservationJson: String?
) {
    val reservation = remember(reservationJson) {
        reservationJson?.let {
            Gson().fromJson(Uri.decode(it), SeatReservationEntity::class.java)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.reservation_details),
                navController = navController,
                showMenu = false
            )
        }
    ) { paddingValues ->
        reservation?.let { res ->
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Column(Modifier.padding(paddingValues).padding(16.dp)) {
                Text("${stringResource(R.string.date)}: ${formatter.format(Date(res.date))}")
                Text("${stringResource(R.string.route)}: ${res.routeId}")
                Text("${stringResource(R.string.start_point)}: ${res.startPoiId}")
                Text("${stringResource(R.string.destination)}: ${res.endPoiId}")
            }
        } ?: Text(
            text = stringResource(R.string.no_reservation_found),
            modifier = Modifier.padding(paddingValues).padding(16.dp)
        )
    }
}
