package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import kotlinx.coroutines.launch

@Composable
fun DrawerMenu(navController: NavController, closeDrawer: () -> Unit) {
    ModalDrawerSheet {
        Text("Menu", modifier = Modifier.padding(16.dp))
        Divider()
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = {
                navController.navigate("settings")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("About") },
            selected = false,
            onClick = {
                navController.navigate("about")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Support") },
            selected = false,
            onClick = {
                navController.navigate("support")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Email, contentDescription = null) }
        )
        val activity = (LocalContext.current as? Activity)
        NavigationDrawerItem(
            label = { Text("Exit") },
            selected = false,
            onClick = {
                activity?.finishAffinity()
            },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerWrapper(
    navController: NavController,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(navController) { scope.launch { drawerState.close() } }
        }
    ) {
        content { scope.launch { drawerState.open() } }
    }
}
