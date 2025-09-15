package com.ioannapergamali.mysmartroute.view.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val sameNameGroups by viewModel.sameNamePois.collectAsState()

    Scaffold(topBar = { TopBar(title = stringResource(R.string.review_poi), navController = navController, showMenu = true, onMenuClick = openDrawer) }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (duplicateGroups.isEmpty() && sameNameGroups.isEmpty()) {
                Text(text = stringResource(R.string.no_duplicate_pois), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(duplicateGroups) { group ->
                        var selected by remember { mutableStateOf<PoIEntity?>(null) }
                        var edited by remember { mutableStateOf("") }

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
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    RadioButton(
                                        selected = selected?.id == poi.id,
                                        onClick = {
                                            selected = poi
                                            edited = poi.name
                                            val uri = Uri.parse("geo:${poi.lat},${poi.lng}?q=" + Uri.encode(poi.name))
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(intent)
                                        }
                                    )
                                    if (selected?.id == poi.id) {
                                        TextField(
                                            value = edited,
                                            onValueChange = { edited = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(text = poi.name)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    selected?.let { keep ->
                                        val updated = keep.copy(name = edited)
                                        viewModel.updatePoi(updated)
                                        group.filter { it.id != updated.id }.forEach { other ->
                                            viewModel.mergePois(updated.id, other.id)
                                        }
                                    }
                                    selected = null
                                },
                                enabled = selected != null,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.keep))
                            }
                        }
                    }

                    if (sameNameGroups.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.same_name_pois),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(sameNameGroups) { group ->
                            var selected by remember { mutableStateOf<PoIEntity?>(null) }
                            var edited by remember { mutableStateOf("") }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                group.forEach { poi ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        RadioButton(
                                            selected = selected?.id == poi.id,
                                            onClick = {
                                                selected = poi
                                                edited = poi.name
                                                val uri = Uri.parse("geo:${poi.lat},${poi.lng}?q=" + Uri.encode(poi.name))
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(intent)
                                            }
                                        )
                                        if (selected?.id == poi.id) {
                                            TextField(
                                                value = edited,
                                                onValueChange = { edited = it },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(text = poi.name)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        selected?.let { keep ->
                                            val updated = keep.copy(name = edited)
                                            viewModel.updatePoi(updated)
                                            group.filter { it.id != updated.id }.forEach { other ->
                                                viewModel.mergePois(updated.id, other.id)
                                            }
                                        }
                                        selected = null
                                    },
                                    enabled = selected != null,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(stringResource(R.string.keep))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
