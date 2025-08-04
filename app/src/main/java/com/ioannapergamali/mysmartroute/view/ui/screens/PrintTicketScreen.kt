package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.viewmodel.UserReservationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrintTicketScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: UserReservationsViewModel = viewModel()
    val reservations: List<SeatReservationEntity> by viewModel.reservations.collectAsState(initial = emptyList())
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.print_ticket),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->

            if (reservations.isEmpty()) {
                Text(text = stringResource(R.string.no_reservations))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(reservations) { res ->
                        ReservationItem(res)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

@Composable
fun ReservationItem(reservation: SeatReservationEntity) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateText = formatter.format(Date(reservation.date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = dateText,
            modifier = Modifier.padding(16.dp)
        )
    }
}

