package com.ioannapergamali.mysmartroute.view.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.utils.toSeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.insertUserSafely
import kotlinx.coroutines.tasks.await
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
    var driverName by remember { mutableStateOf("") }
    var passengerName by remember { mutableStateOf("") }
    var detailInfos by remember { mutableStateOf<List<DetailInfo>>(emptyList()) }

    LaunchedEffect(reservation) {
        reservation?.let { res ->
            val db = MySmartRouteDatabase.getInstance(context)
            routeName = db.routeDao().findById(res.routeId)?.name ?: res.routeId
            val detailDocs = FirebaseFirestore.getInstance()
                .collection("seat_reservations")
                .document(res.id)
                .collection("details")
                .get()
                .await()
            val details = detailDocs.documents.mapNotNull { it.toSeatReservationDetailEntity(res.id) }
            detailInfos = details.map { det ->
                val start = db.poIDao().findById(det.startPoiId)?.name ?: det.startPoiId
                val end = db.poIDao().findById(det.endPoiId)?.name ?: det.endPoiId
                DetailInfo(start, end, det.cost, det.startTime)
            }
            val decl = db.transportDeclarationDao().getById(res.declarationId)
            driverName = decl?.driverId?.let { driverId ->
                val user = db.userDao().getUser(driverId)
                    ?: FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(driverId)
                        .get()
                        .await()
                        .toUserEntity()
                        ?.also { insertUserSafely(db.userDao(), it) }
                user?.let { "${it.name} ${it.surname}" } ?: driverId
            }.orEmpty()
            val localPassenger = db.userDao().getUser(res.userId)
            passengerName = if (localPassenger == null ||
                (localPassenger.name.isBlank() && localPassenger.surname.isBlank())
            ) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(res.userId)
                    .get()
                    .await()
                    .toUserEntity()
                    ?.also { insertUserSafely(db.userDao(), it) }
                    ?.let { user ->
                        listOf(user.name, user.surname)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { user.username.takeIf { it.isNotBlank() } ?: res.userId }
                    } ?: res.userId
            } else {
                listOf(localPassenger.name, localPassenger.surname)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { localPassenger.username.takeIf { it.isNotBlank() } ?: res.userId }
            }

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
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            Card(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ticket))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.date)}: ${formatter.format(Date(res.date))}")
                    Text("${stringResource(R.string.route)}: $routeName")
                    Text("${stringResource(R.string.driver)}: $driverName")
                    Text("${stringResource(R.string.passenger)}: $passengerName")
                    detailInfos.forEach { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${stringResource(R.string.start_point)}: ${info.start}")
                        Text("${stringResource(R.string.destination)}: ${info.end}")
                        Text("${stringResource(R.string.cost)}: ${info.cost}")
                        Text("${stringResource(R.string.time)}: ${timeFormatter.format(Date(info.startTime))}")
                    }
                }
            }
        } ?: Text(
            text = stringResource(R.string.no_reservation_found),
            modifier = Modifier.padding(paddingValues).padding(16.dp)
        )
    }
}

private data class DetailInfo(
    val start: String,
    val end: String,
    val cost: Double,
    val startTime: Long,
)
