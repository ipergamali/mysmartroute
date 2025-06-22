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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel

@Composable
fun FirebaseDatabaseScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val data by viewModel.firebaseData.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadFirebaseData() }

    Scaffold(
        topBar = {
            TopBar(
                title = "Firebase DB",
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
                items(data!!.users) { user ->
                    Text("ID: ${user.id}, ${user.name} ${user.surname}, ${user.username}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Vehicles", style = MaterialTheme.typography.titleMedium) }
                items(data!!.vehicles) { vehicle ->
                    Text("${vehicle.description}, τύπος ${vehicle.type}, θέσεις ${vehicle.seat}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("PoIs", style = MaterialTheme.typography.titleMedium) }
                items(data!!.pois) { poi ->
                    Text("${poi.name} (${poi.type}) - ${poi.description}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Settings", style = MaterialTheme.typography.titleMedium) }
                items(data!!.settings) { setting ->
                    Text("${setting.userId} -> ${setting.theme}, dark ${setting.darkTheme}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Roles", style = MaterialTheme.typography.titleMedium) }
                items(data!!.roles) { role ->
                    Text("${role.id} - ${role.name}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Menus", style = MaterialTheme.typography.titleMedium) }
                items(data!!.menus) { menu ->
                    Text("${menu.id} (${menu.roleId}) - ${menu.title}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Menu Options", style = MaterialTheme.typography.titleMedium) }
                items(data!!.menuOptions) { opt ->
                    Text("${opt.id} (${opt.menuId}) -> ${opt.title} -> ${opt.route}")
                }
                item { Spacer(modifier = Modifier.padding(8.dp)) }
                item { Text("Authentication table δεν είναι διαθέσιμη από το client", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
