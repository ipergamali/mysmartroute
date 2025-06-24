package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navController: NavController,
    showMenu: Boolean = false,
    showLogout: Boolean = false,
    showBack: Boolean = true,
    showHomeIcon: Boolean = true,
    onMenuClick: () -> Unit = {},
    onLogout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
    }
) {
    val username = remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        username.value = doc.getString("username")
                    }
            } else {
                username.value = null
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.statusBarsPadding()) {
        TopAppBar(
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
        title = { Text(title) },
        navigationIcon = {
            Row {
                IconButton(onClick = {
                    if (showMenu) onMenuClick() else navController.popBackStack()
                }) {
                    Icon(
                        if (showMenu) Icons.Filled.Menu else Icons.AutoMirrored.Filled.List,
                        contentDescription = if (showMenu) "menu" else "list",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showHomeIcon) {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showBack) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            username.value?.let { name ->
                Box {
                    Text(
                        name,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { menuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Προφίλ") }, onClick = {
                            menuExpanded = false
                            navController.navigate("profile")
                        })
                        DropdownMenuItem(text = { Text("Μενού χρήστη") }, onClick = {
                            menuExpanded = false
                            navController.navigate("menu")
                        })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = {
                            menuExpanded = false
                            navController.navigate("settings")
                        })
                        DropdownMenuItem(text = { Text("Logout") }, onClick = {
                            menuExpanded = false
                            onLogout()
                        })
                    }
                }
            }
            if (showLogout) {
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = "logout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        )
    }
}
