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
import android.util.Log
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
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import androidx.compose.runtime.remember

private const val TAG = "MenuScreen"

@Composable
fun MenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: AuthenticationViewModel = viewModel()
    val menus by viewModel.currentMenus.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Log.d(TAG, "Loading user role and menus")
        viewModel.loadCurrentUserRole(context, loadMenus = true)
    }

    LaunchedEffect(role, menus) {
        Log.d(TAG, "Role: $role, menus size: ${menus.size}")
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.menu_title),
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
                    val targetRoute = if (route == "definePoi") {
                        "definePoi?lat=&lng=&source=&view=false"
                    } else {
                        route
                    }
                    if (
                        targetRoute == "viewUsers" ||
                        (targetRoute.isNotEmpty() && navController.graph.any {
                            it.route == "definePoi?lat={lat}&lng={lng}&source={source}&view={view}" ||
                                it.route == targetRoute
                        })
                    ) {
                        navController.navigate(targetRoute)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.not_implemented),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
            text = stringResource(R.string.menu_role_prefix, role?.localizedName() ?: ""),
            style = MaterialTheme.typography.titleLarge
        )
        val descKey = when (role) {
            UserRole.PASSENGER -> "role_passenger_desc"
            UserRole.DRIVER -> "role_driver_desc"
            UserRole.ADMIN -> null
            else -> null
        }
        descKey?.let { key ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = resolveString(key),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            LazyColumn {
                menus.forEach { menuWithOptions ->
                    item {
                        Text(
                            text = resolveString(menuWithOptions.menu.titleResKey),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    items(menuWithOptions.options) { option ->
                        MenuItemRow(option.titleResKey) { onOptionClick(option.route) }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(titleKey: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = resolveString(titleKey),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun resolveString(key: String): String {
    val context = LocalContext.current
    val resId = remember(key) {
        context.resources.getIdentifier(key, "string", context.packageName)
    }
    return if (resId != 0) {
        stringResource(id = resId)
    } else {
        key
    }
}
