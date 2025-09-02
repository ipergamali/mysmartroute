package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserPointViewModel

/**
 * Απλή οθόνη που εμφανίζει τα ονόματα όλων των σημείων που έχουν
 * προσθέσει οι χρήστες. Η λίστα ενημερώνεται αυτόματα όταν αλλάξουν
 * τα δεδομένα στο [UserPointViewModel].
 */
@Composable
fun UserPointsScreen(viewModel: UserPointViewModel = viewModel()) {
    val pointsState = viewModel.points.collectAsState()

    LazyColumn {
        items(pointsState.value) { point ->
            Text(text = point.name)
        }
    }
}
