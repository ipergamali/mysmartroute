package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.data.local.MenuWithOptions

@Composable
fun MenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: AuthenticationViewModel = viewModel()
    val menus by viewModel.currentMenus.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserRole()
        viewModel.loadCurrentUserMenus(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Menu",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer,
                onLogout = {
                    viewModel.signOut()
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            if (menus.isEmpty()) {
                CircularProgressIndicator()
            } else {
                MenuEntityTable(menus) { route ->
                    if (route.isNotEmpty()) navController.navigate(route)
                    else Toast.makeText(context, "Η λειτουργία δεν είναι διαθέσιμη", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun MenuEntityTable(menus: List<MenuWithOptions>, onRouteSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        menus.forEachIndexed { index, menuWithOptions ->
            Text(text = "${index + 1}. ${menuWithOptions.menu.title}", style = MaterialTheme.typography.titleMedium)
            menuWithOptions.options.forEach { option ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 2.dp, bottom = 2.dp)) {
                    Text(
                        text = option.title,
                        modifier = Modifier
                            .clickable { onRouteSelected(option.route) }
                            .weight(1f)
                    )
                }
            }
        }
    }
}
