package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.repository.Point
import com.ioannapergamali.mysmartroute.viewmodel.UserPointViewModel

/**
 * Απλή οθόνη που εμφανίζει τα ονόματα όλων των σημείων που έχουν
 * προσθέσει οι χρήστες. Η λίστα ενημερώνεται αυτόματα όταν αλλάξουν
 * τα δεδομένα στο [UserPointViewModel].
 */
@Composable
fun UserPointsScreen(viewModel: UserPointViewModel = viewModel()) {
    val pointsState = viewModel.points.collectAsState()
    var editingPoint by remember { mutableStateOf<Point?>(null) }
    var mergingPoint by remember { mutableStateOf<Point?>(null) }

    LazyColumn {
        items(pointsState.value) { point ->
            Row {
                Column {
                    Text(text = point.name)
                    if (point.details.isNotEmpty()) {
                        Text(text = point.details)
                    }
                }
                Button(onClick = { editingPoint = point }) {
                    Text("Επεξεργασία")
                }
                Button(onClick = { mergingPoint = point }) {
                    Text("Συγχώνευση")
                }
            }
        }
    }

    editingPoint?.let { point ->
        var name by remember { mutableStateOf(point.name) }
        var details by remember { mutableStateOf(point.details) }

        AlertDialog(
            onDismissRequest = { editingPoint = null },
            title = { Text("Επεξεργασία σημείου") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Όνομα") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Στοιχεία") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePoint(point.id, name, details)
                    editingPoint = null
                }) { Text("Αποθήκευση") }
            },
            dismissButton = {
                TextButton(onClick = { editingPoint = null }) { Text("Άκυρο") }
            }
        )
    }

    mergingPoint?.let { point ->
        AlertDialog(
            onDismissRequest = { mergingPoint = null },
            title = { Text("Συγχώνευση με...") },
            text = {
                LazyColumn {
                    items(pointsState.value.filter { it.id != point.id }) { other ->
                        TextButton(onClick = {
                            viewModel.mergePoints(point.id, other.id)
                            mergingPoint = null
                        }) {
                            Text(other.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mergingPoint = null }) { Text("Άκυρο") }
            }
        )
    }
}
