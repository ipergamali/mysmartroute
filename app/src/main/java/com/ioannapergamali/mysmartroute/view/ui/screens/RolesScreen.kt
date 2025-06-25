package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RoleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.SyncState
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

@Composable
fun RolesScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: RoleViewModel = viewModel()
    val dbViewModel: DatabaseViewModel = viewModel()
    val roles by viewModel.roles.collectAsState()
    val syncState by dbViewModel.syncState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        dbViewModel.syncDatabases(context)
    }

    LaunchedEffect(syncState) {
        if (syncState !is SyncState.Loading) {
            viewModel.loadRoles(context)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.roles_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (roles.isEmpty() && syncState is SyncState.Loading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(roles) { role ->
                        Text("${'$'}{role.id} - ${'$'}{role.name}")
                    }
                }
            }
        }
    }
}
