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
import com.ioannapergamali.mysmartroute.viewmodel.DriverRatingViewModel

@Composable
fun RankDriversScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DriverRatingViewModel = viewModel()
    val best by viewModel.bestDrivers.collectAsState()
    val worst by viewModel.worstDrivers.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadRatings(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.rank_drivers),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Text(stringResource(R.string.top_drivers), style = MaterialTheme.typography.titleMedium)
            if (best.isEmpty()) {
                Text(stringResource(R.string.no_driver_ratings))
            } else {
                best.forEachIndexed { index, driver ->
                    Text("${index + 1}. ${driver.name} ${driver.surname} - ${stringResource(R.string.average_rating_label, driver.averageRating)}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.worst_drivers), style = MaterialTheme.typography.titleMedium)
            if (worst.isEmpty()) {
                Text(stringResource(R.string.no_driver_ratings))
            } else {
                worst.forEachIndexed { index, driver ->
                    Text("${index + 1}. ${driver.name} ${driver.surname} - ${stringResource(R.string.average_rating_label, driver.averageRating)}")
                }
            }
        }
    }
}
