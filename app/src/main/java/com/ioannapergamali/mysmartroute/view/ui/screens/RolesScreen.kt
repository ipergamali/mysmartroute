package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import android.util.Log

@Composable
fun RolesScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: RoleViewModel = viewModel()
    val roles by viewModel.roles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadRoles(context) }

    LaunchedEffect(roles) {
        Log.d("RolesScreen", "Roles count: ${'$'}{roles.size}")
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Roles",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(roles) { role ->
                    Text("${'$'}{role.id} - ${'$'}{role.name}")
                }
            }
        }
    }
}
