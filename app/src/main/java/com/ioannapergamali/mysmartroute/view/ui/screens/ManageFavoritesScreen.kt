package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.util.iconForVehicle
import com.ioannapergamali.mysmartroute.view.ui.util.labelForVehicle
import com.ioannapergamali.mysmartroute.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFavoritesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: FavoritesViewModel = viewModel()
    val preferred by viewModel.preferredFlow(context).collectAsState(initial = emptySet())
    val nonPreferred by viewModel.nonPreferredFlow(context).collectAsState(initial = emptySet())

    var prefExpanded by remember { mutableStateOf(false) }
    var nonPrefExpanded by remember { mutableStateOf(false) }
    var selectedPref by remember { mutableStateOf(VehicleType.CAR) }
    var selectedNonPref by remember { mutableStateOf(VehicleType.CAR) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.manage_favorites),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Text(stringResource(R.string.favorites_preferred))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = prefExpanded, onExpandedChange = { prefExpanded = !prefExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = labelForVehicle(selectedPref),
                        onValueChange = {},
                        modifier = Modifier.menuAnchor().weight(1f),
                        label = { Text(stringResource(R.string.vehicle_type)) },
                        leadingIcon = { Icon(iconForVehicle(selectedPref), contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prefExpanded) }
                    )
                    DropdownMenu(expanded = prefExpanded, onDismissRequest = { prefExpanded = false }) {
                        VehicleType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(labelForVehicle(type)) },
                                leadingIcon = { Icon(iconForVehicle(type), contentDescription = null) },
                                onClick = {
                                    selectedPref = type
                                    prefExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.addPreferred(context, selectedPref) }) {
                    Text(stringResource(R.string.add_favorite))
                }
            }
            preferred.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = iconForVehicle(type),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(labelForVehicle(type), modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removePreferred(context, type) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_favorite))
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.favorites_non_preferred))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = nonPrefExpanded, onExpandedChange = { nonPrefExpanded = !nonPrefExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = labelForVehicle(selectedNonPref),
                        onValueChange = {},
                        modifier = Modifier.menuAnchor().weight(1f),
                        label = { Text(stringResource(R.string.vehicle_type)) },
                        leadingIcon = { Icon(iconForVehicle(selectedNonPref), contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nonPrefExpanded) }
                    )
                    DropdownMenu(expanded = nonPrefExpanded, onDismissRequest = { nonPrefExpanded = false }) {
                        VehicleType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(labelForVehicle(type)) },
                                leadingIcon = { Icon(iconForVehicle(type), contentDescription = null) },
                                onClick = {
                                    selectedNonPref = type
                                    nonPrefExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.addNonPreferred(context, selectedNonPref) }) {
                    Text(stringResource(R.string.add_favorite))
                }
            }
            nonPreferred.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = iconForVehicle(type),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(labelForVehicle(type), modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeNonPreferred(context, type) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_favorite))
                    }
                }
            }
        }
    }
}
