package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.classes.transports.TripWithRating
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TripRatingViewModel
import android.text.format.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankTransportsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: TripRatingViewModel = viewModel()
    val trips by viewModel.trips.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTrips(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.rank_transports),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (trips.isEmpty()) {
                Text(stringResource(R.string.no_completed_transports))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(trips) { trip ->
                        TripRatingItem(trip) { rating, comment ->
                            viewModel.updateRating(context, trip.moving, rating, comment)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TripRatingItem(
    trip: TripWithRating,
    onSave: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(trip.rating.toFloat()) }
    var comment by remember { mutableStateOf(trip.comment) }
    val context = LocalContext.current
    val dateText = if (trip.moving.date > 0L) {
        DateFormat.getDateFormat(context).format(Date(trip.moving.date))
    } else ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = trip.moving.routeName, style = MaterialTheme.typography.titleMedium)
        if (dateText.isNotBlank()) {
            Text(text = dateText, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = rating,
            onValueChange = {
                rating = it
                onSave(it.toInt(), comment)
            },
            valueRange = 0f..100f
        )
        Text(stringResource(R.string.rating_label, rating.toInt()))
        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
                onSave(rating.toInt(), comment)
            },
            label = { Text(stringResource(R.string.comment_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
