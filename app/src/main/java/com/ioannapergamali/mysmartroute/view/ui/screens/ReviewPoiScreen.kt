package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AdminPoiViewModel
import androidx.compose.ui.res.stringResource

/**
 * Οθόνη ελέγχου και διαχείρισης ονομάτων PoI από τον διαχειριστή.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPoiScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AdminPoiViewModel = viewModel(factory = AdminPoiViewModel.Factory(context))
    val pois by viewModel.pois.collectAsState()

    var editPoi by remember { mutableStateOf<PoIEntity?>(null) }
    var editedName by remember { mutableStateOf("") }
    var mergeKeepId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopBar(title = stringResource(R.string.review_poi), navController = navController, showMenu = true, onMenuClick = openDrawer) }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pois) { poi ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${poi.name} (${poi.lat}, ${poi.lng})")
                        Row {
                            IconButton(onClick = {
                                editPoi = poi
                                editedName = poi.name
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Επεξεργασία")
                            }
                            IconButton(onClick = {
                                mergeKeepId?.let { keep ->
                                    if (keep != poi.id) {
                                        viewModel.mergePois(keep, poi.id)
                                    }
                                    mergeKeepId = null
                                } ?: run {
                                    mergeKeepId = poi.id
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Συγχώνευση",
                                    tint = if (mergeKeepId == poi.id)
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.deletePoi(poi.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Διαγραφή")
                            }
                        }
                    }
                }
            }
        }
    }

    editPoi?.let { poi ->
        AlertDialog(
            onDismissRequest = { editPoi = null },
            title = { Text(stringResource(R.string.poi_name)) },
            text = {
                TextField(value = editedName, onValueChange = { editedName = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePoi(poi.copy(name = editedName))
                    editPoi = null
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { editPoi = null }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}
