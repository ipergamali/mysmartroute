package com.ioannapergamali.mysmartroute.view.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
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

    val context = LocalContext.current
    var routeName by remember { mutableStateOf("") }
    var startPoiName by remember { mutableStateOf("") }
    var endPoiName by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var passengerName by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(reservation) {
        reservation?.let { res ->
            val db = MySmartRouteDatabase.getInstance(context)
            routeName = db.routeDao().findById(res.routeId)?.name ?: res.routeId
            startPoiName = db.poIDao().findById(res.startPoiId)?.name ?: res.startPoiId
            endPoiName = db.poIDao().findById(res.endPoiId)?.name ?: res.endPoiId
            val decl = db.transportDeclarationDao().getById(res.declarationId)
            cost = decl?.cost
            driverName = decl?.driverId?.let { db.userDao().getUser(it) }
                ?.let { "${it.name} ${it.surname}" } ?: decl?.driverId.orEmpty()
            passengerName = db.userDao().getUser(res.userId)
                ?.let { "${it.name} ${it.surname}" } ?: res.userId
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
                Text("${stringResource(R.string.route)}: $routeName")
                Text("${stringResource(R.string.start_point)}: $startPoiName")
                Text("${stringResource(R.string.destination)}: $endPoiName")
                Text("${stringResource(R.string.driver)}: $driverName")
                cost?.let { Text("${stringResource(R.string.cost)}: $it") }
                Text("${stringResource(R.string.passenger)}: $passengerName")
            }
        } ?: Text(
            text = stringResource(R.string.no_reservation_found),
            modifier = Modifier.padding(paddingValues).padding(16.dp)
        )
    }
}
