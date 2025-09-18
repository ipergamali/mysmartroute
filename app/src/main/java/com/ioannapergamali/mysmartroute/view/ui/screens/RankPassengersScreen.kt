package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PassengerSatisfactionViewModel

@Composable
fun RankPassengersScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PassengerSatisfactionViewModel = viewModel()
    val mostSatisfied by viewModel.mostSatisfied.collectAsState()
    val leastSatisfied by viewModel.leastSatisfied.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadRatings(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.rank_passengers),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Text(
                stringResource(R.string.most_satisfied_passengers),
                style = MaterialTheme.typography.titleMedium
            )
            if (mostSatisfied.isEmpty()) {
                Text(stringResource(R.string.no_passenger_ratings))
            } else {
                mostSatisfied.forEachIndexed { index, passenger ->
                    Text(
                        "${index + 1}. ${passenger.name} ${passenger.surname} - " +
                            stringResource(R.string.average_rating_label, passenger.averageRating)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.least_satisfied_passengers),
                style = MaterialTheme.typography.titleMedium
            )
            if (leastSatisfied.isEmpty()) {
                Text(stringResource(R.string.no_passenger_ratings))
            } else {
                leastSatisfied.forEachIndexed { index, passenger ->
                    Text(
                        "${index + 1}. ${passenger.name} ${passenger.surname} - " +
                            stringResource(R.string.average_rating_label, passenger.averageRating)
                    )
                }
            }
        }
    }
}
