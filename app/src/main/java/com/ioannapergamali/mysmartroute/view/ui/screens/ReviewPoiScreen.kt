package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AdminPoiViewModel

/**
 * Οθόνη ελέγχου και διαχείρισης ονομάτων PoI από τον διαχειριστή.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPoiScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AdminPoiViewModel = viewModel(factory = AdminPoiViewModel.Factory(context))
    val duplicateGroups by viewModel.duplicatePois.collectAsState()

    var selectedGroup by remember { mutableStateOf<List<PoIEntity>?>(null) }
    var keepPoi by remember { mutableStateOf<PoIEntity?>(null) }
    var editedName by remember { mutableStateOf("") }

    Scaffold(topBar = { TopBar(title = stringResource(R.string.review_poi), navController = navController, showMenu = true, onMenuClick = openDrawer) }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (duplicateGroups.isEmpty()) {
                Text(text = stringResource(R.string.no_duplicate_pois), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(duplicateGroups) { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            val coord = group.first()
                            Text(text = stringResource(R.string.coordinates_label))
                            Text(text = "${stringResource(R.string.lat)}: ${coord.lat}")
                            Text(text = "${stringResource(R.string.lng)}: ${coord.lng}")
                            group.forEach { poi ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${stringResource(R.string.poi_name)}: ${poi.name}")
                                    IconButton(onClick = {
                                        selectedGroup = group
                                        keepPoi = poi
                                        editedName = poi.name
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (keepPoi != null && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = {
                keepPoi = null
                selectedGroup = null
            },
            title = { Text(stringResource(R.string.poi_name)) },
            text = { TextField(value = editedName, onValueChange = { editedName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    val updatedPoi = keepPoi!!.copy(name = editedName)
                    viewModel.updatePoi(updatedPoi)
                    selectedGroup!!.filter { it.id != updatedPoi.id }.forEach { other ->
                        viewModel.mergePois(updatedPoi.id, other.id)
                    }
                    keepPoi = null
                    selectedGroup = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    keepPoi = null
                    selectedGroup = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
