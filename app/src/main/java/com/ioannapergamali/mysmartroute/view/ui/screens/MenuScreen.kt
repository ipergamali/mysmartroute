package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

@Composable
fun MenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: AuthenticationViewModel = viewModel()
    val menus by viewModel.currentMenus.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()

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
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (menus.isEmpty()) {
                CircularProgressIndicator()
            } else {
                RoleMenu(role, menus) { route ->
                    if (route.isNotEmpty()) navController.navigate(route)
                    else Toast.makeText(context, "Η λειτουργία δεν είναι διαθέσιμη", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun RoleMenu(
    role: UserRole?,
    menus: List<MenuWithOptions>,
    onOptionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Μενού για τον ρόλο: ${role?.name ?: ""}",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            LazyColumn {
                menus.forEach { menuWithOptions ->
                    item {
                        Text(
                            text = menuWithOptions.menu.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider()
                    }
                    items(menuWithOptions.options) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionClick(option.route) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = option.title, style = MaterialTheme.typography.bodyLarge)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
