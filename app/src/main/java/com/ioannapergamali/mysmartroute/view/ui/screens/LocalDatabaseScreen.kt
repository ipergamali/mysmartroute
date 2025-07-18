package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName

@Composable
fun LocalDatabaseScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val context = LocalContext.current
    val data by viewModel.localData.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadLocalData(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.local_db),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        if (data == null) {
            ScreenContainer(modifier = Modifier.padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        } else {
            ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("Users", style = MaterialTheme.typography.titleMedium) }
                if (data!!.users.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.users) { user ->
                        Text("ID: ${user.id}, ${user.name} ${user.surname}, ${user.username}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Vehicles", style = MaterialTheme.typography.titleMedium) }
                if (data!!.vehicles.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.vehicles) { vehicle ->
                        Text("${vehicle.description}, τύπος ${vehicle.type}, θέσεις ${vehicle.seat}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Poi Types", style = MaterialTheme.typography.titleMedium) }
                if (data!!.poiTypes.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.poiTypes) { t ->
                        Text("${t.id} -> ${t.name}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("PoIs", style = MaterialTheme.typography.titleMedium) }
                if (data!!.pois.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.pois) { poi ->
                        Text("${poi.name} (${poi.type}) - ${poi.address.city}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Routes", style = MaterialTheme.typography.titleMedium) }
                if (data!!.routes.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.routes) { r ->
                        Text("${r.id} -> ${r.name}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Route Points", style = MaterialTheme.typography.titleMedium) }
                if (data!!.routePoints.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.routePoints) { p ->
                        Text("${p.routeId} : ${p.position} -> ${p.poiId}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Movings", style = MaterialTheme.typography.titleMedium) }
                if (data!!.movings.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.movings) { m ->
                        Text("${m.id} route:${m.routeId} user:${m.userId}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Transport Declarations", style = MaterialTheme.typography.titleMedium) }
                if (data!!.declarations.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.declarations) { d ->
                        Text("${d.id} route:${d.routeId} type:${d.vehicleType}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Settings", style = MaterialTheme.typography.titleMedium) }
                if (data!!.settings.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.settings) { setting ->
                        Text("${setting.userId} -> ${setting.theme}, dark ${setting.darkTheme}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Roles", style = MaterialTheme.typography.titleMedium) }
                if (data!!.roles.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.roles) { role ->
                        val roleEnum = try {
                            UserRole.valueOf(role.name)
                        } catch (_: Exception) {
                            null
                        }
                        val name = roleEnum?.localizedName() ?: role.name
                        Text("${role.id} - $name")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Menus", style = MaterialTheme.typography.titleMedium) }
                if (data!!.menus.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.menus) { menu ->
                        Text("${menu.id} (${menu.roleId}) - ${menu.titleResKey}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Menu Options", style = MaterialTheme.typography.titleMedium) }
                if (data!!.menuOptions.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.menuOptions) { opt ->
                        Text("${opt.id} (${opt.menuId}) -> ${opt.titleResKey} -> ${opt.route}")
                    }
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleMedium) }
                if (data!!.languages.isEmpty()) {
                    item { Text("Ο πίνακας είναι άδειος") }
                } else {
                    items(data!!.languages) { lang ->
                        Text("${lang.id} -> ${lang.language}")
                    }
                }
                }
            }
        }
    }
}
