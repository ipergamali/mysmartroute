package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import com.ioannapergamali.mysmartroute.view.ui.components.LogoImage
import com.ioannapergamali.mysmartroute.view.ui.components.LogoAssets
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
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
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            LogoImage(
                drawableRes = LogoAssets.LOGO,
                contentDescription = "logo",
                modifier = Modifier.padding(end = 8.dp),
                size = 36.dp
            )
            Text(
                "Menu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        val context = LocalContext.current
        val user = FirebaseAuth.getInstance().currentUser

        user?.email?.let { email ->
            Text(email, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
        } ?: Divider(color = MaterialTheme.colorScheme.outline)

        
        NavigationDrawerItem(
            label = { Text("About") },
            selected = false,
            onClick = {
                navController.navigate("about")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text("Support") },
            selected = false,
            onClick = {
                navController.navigate("support")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text("Databases") },
            selected = false,
            onClick = {
                navController.navigate("databaseMenu")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text("Admin Option") },
            selected = false,
            onClick = {
                navController.navigate("adminSignup")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text("Admin Dashboard") },
            selected = false,
            onClick = {
                navController.navigate("adminDashboard")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        if (user != null) {
            NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
        val activity = (LocalContext.current as? Activity)
        NavigationDrawerItem(
            label = { Text("Exit") },
            selected = false,
            onClick = {
                activity?.finishAffinity()
            },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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
        },
        scrimColor = MaterialTheme.colorScheme.scrim
    ) {
        content { scope.launch { drawerState.open() } }
    }
}
