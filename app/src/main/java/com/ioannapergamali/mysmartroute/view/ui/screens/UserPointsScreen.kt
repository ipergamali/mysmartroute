package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.repository.Point
import com.ioannapergamali.mysmartroute.viewmodel.UserPointViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Απλή οθόνη που εμφανίζει τα ονόματα όλων των σημείων που έχουν
 * προσθέσει οι χρήστες. Η λίστα ενημερώνεται αυτόματα όταν αλλάξουν
 * τα δεδομένα στο [UserPointViewModel].
 */
@Composable
fun UserPointsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    viewModel: UserPointViewModel = viewModel()
) {
    val poiViewModel: PoIViewModel = viewModel()
    val context = LocalContext.current
    val pois by poiViewModel.pois.collectAsState()
    LaunchedEffect(Unit) { poiViewModel.loadPois(context) }
    val pointsState = viewModel.points.collectAsState()
    val availablePois by remember(pois, pointsState.value) {
        mutableStateOf(viewModel.availablePois(pois))
    }
    var editingPoint by remember { mutableStateOf<Point?>(null) }
    var mergingPoint by remember { mutableStateOf<Point?>(null) }
    var addingPoint by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Σημεία χρηστών",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingPoint = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Προσθήκη")
            }
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (pointsState.value.isEmpty()) {
                Text("Δεν υπάρχουν σημεία")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                            Button(onClick = { viewModel.deletePoint(point.id) }) {
                                Text("Διαγραφή")
                            }
                        }
                    }
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

    if (addingPoint) {
        AlertDialog(
            onDismissRequest = { addingPoint = false },
            title = { Text("Επιλογή POI") },
            text = {
                if (availablePois.isEmpty()) {
                    Text("Δεν υπάρχουν διαθέσιμα POI")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availablePois) { poi ->
                            TextButton(onClick = {
                                viewModel.addPoint(Point(poi.id, poi.name, ""))
                                addingPoint = false
                            }) {
                                Text(poi.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { addingPoint = false }) { Text("Άκυρο") }
            }
        )
    }

    mergingPoint?.let { point ->
        AlertDialog(
            onDismissRequest = { mergingPoint = null },
            title = { Text("Συγχώνευση με...") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
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
